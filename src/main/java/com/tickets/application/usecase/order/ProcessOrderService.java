package com.tickets.application.usecase.order;

import com.tickets.application.port.in.order.ProcessOrderUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketReservationRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.domain.exception.InvalidOrderException;
import com.tickets.domain.exception.OrderNotFoundException;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.domain.model.ticket.TicketTransition;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of {@link ProcessOrderUseCase}.
 *
 * <p>Loads the order, finds available tickets for the event, and attempts an
 * atomic reservation via DynamoDB TransactWriteItems. If the transaction
 * succeeds, saves audit transitions and transitions the order to RESERVED.
 * If the Event condition fails (not enough availability), rejects the order.
 * If a Ticket condition fails (tickets claimed by another transaction),
 * retries with a fresh set of available tickets (up to {@value #MAX_RETRIES} times).</p>
 */
public final class ProcessOrderService implements ProcessOrderUseCase {

    private static final int RESERVATION_TTL_MINUTES = 10;
    private static final int MAX_RETRIES = 3;
    private static final int MAX_TICKETS_PER_ORDER = 99;

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final TicketTransitionRepository transitionRepository;
    private final TicketReservationRepository ticketReservationRepository;

    public ProcessOrderService(OrderRepository orderRepository,
                               TicketRepository ticketRepository,
                               EventRepository eventRepository,
                               TicketTransitionRepository transitionRepository,
                               TicketReservationRepository ticketReservationRepository) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.transitionRepository = transitionRepository;
        this.ticketReservationRepository = ticketReservationRepository;
    }

    @Override
    public Mono<Order> process(ProcessOrderCommand command) {
        if (command.quantity() > MAX_TICKETS_PER_ORDER) {
            return Mono.error(new InvalidOrderException(
                    "Quantity exceeds maximum of " + MAX_TICKETS_PER_ORDER + " tickets per order"));
        }
        return orderRepository.findById(command.orderId())
                .switchIfEmpty(Mono.error(new OrderNotFoundException(
                        "Order not found with id: " + command.orderId())))
                .flatMap(order -> reserveWithRetries(order, command, 1));
    }

    private Mono<Order> reserveWithRetries(Order order, ProcessOrderCommand command, int attempt) {
        if (attempt > MAX_RETRIES) {
            return rejectOrder(order);
        }
        return ticketRepository.findAvailableByEvent(command.eventId(), command.quantity())
                .collectList()
                .flatMap(availableTickets -> {
                    if (availableTickets.size() < command.quantity()) {
                        return rejectOrder(order);
                    }
                    return attemptReservation(order, command, availableTickets, attempt);
                });
    }

    private Mono<Order> attemptReservation(Order order, ProcessOrderCommand command,
                                           List<Ticket> availableTickets, int attempt) {
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES);
        String orderIdStr = order.id().value().toString();

        List<Ticket> reservedTickets = availableTickets.stream()
                .limit(command.quantity())
                .map(ticket -> ticket.reserve(orderIdStr, command.customerId(), reservedUntil))
                .toList();

        Order reservedOrder = order.reserve(
                reservedTickets.stream().map(Ticket::id).toList(),
                reservedUntil);

        return ticketReservationRepository.reserveTicketsAtomically(
                        command.eventId(), command.quantity(), reservedTickets)
                .flatMap(result -> switch (result) {
                    case ReservationResult.Success ignored ->
                            saveTransitions(reservedTickets, TicketStatus.AVAILABLE, TicketStatus.RESERVED, orderIdStr)
                                    .then(orderRepository.save(reservedOrder));
                    case ReservationResult.EventConditionFailed ignored ->
                            rejectOrder(order);
                    case ReservationResult.TicketConditionFailed ignored ->
                            reserveWithRetries(order, command, attempt + 1);
                });
    }

    private Mono<Order> rejectOrder(Order order) {
        Order rejected = order.reject();
        return orderRepository.save(rejected);
    }

    private Mono<Void> saveTransitions(List<Ticket> tickets, TicketStatus from, TicketStatus to,
                                       String orderId) {
        List<TicketTransition> transitions = tickets.stream()
                .map(ticket -> TicketTransition.create(ticket.id(), from, to, orderId))
                .toList();
        return transitionRepository.saveAll(transitions);
    }
}
