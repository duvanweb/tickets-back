package com.tickets.application.usecase.order;

import com.tickets.application.port.in.order.ProcessOrderUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
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
 * <p>Loads the order, finds available tickets for the event, and either:
 * <ul>
 *   <li>Reserves N tickets (AVAILABLE → RESERVED with 10-minute TTL), updates event counters,
 *       and transitions the order to RESERVED — if enough tickets are available.</li>
 *   <li>Rejects the order (PENDING → REJECTED) — if not enough tickets are available.</li>
 * </ul></p>
 */
public final class ProcessOrderService implements ProcessOrderUseCase {

    private static final int RESERVATION_TTL_MINUTES = 10;

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final TicketTransitionRepository transitionRepository;

    public ProcessOrderService(OrderRepository orderRepository,
                               TicketRepository ticketRepository,
                               EventRepository eventRepository,
                               TicketTransitionRepository transitionRepository) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.transitionRepository = transitionRepository;
    }

    @Override
    public Mono<Order> process(ProcessOrderCommand command) {
        return orderRepository.findById(command.orderId())
                .switchIfEmpty(Mono.error(new OrderNotFoundException(
                        "Order not found with id: " + command.orderId())))
                .flatMap(order -> reserveOrReject(order, command));
    }

    private Mono<Order> reserveOrReject(Order order, ProcessOrderCommand command) {
        return ticketRepository.findAvailableByEvent(command.eventId(), command.quantity())
                .collectList()
                .flatMap(availableTickets -> {
                    if (availableTickets.size() < command.quantity()) {
                        return rejectOrder(order);
                    }
                    return reserveOrder(order, command, availableTickets);
                });
    }

    private Mono<Order> rejectOrder(Order order) {
        Order rejected = order.reject();
        return orderRepository.save(rejected);
    }

    private Mono<Order> reserveOrder(Order order, ProcessOrderCommand command,
                                     List<Ticket> availableTickets) {
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES);
        String orderIdStr = order.id().value().toString();

        List<Ticket> reservedTickets = availableTickets.stream()
                .limit(command.quantity())
                .map(ticket -> ticket.reserve(orderIdStr, command.customerId(), reservedUntil))
                .toList();

        Order reservedOrder = order.reserve(
                reservedTickets.stream().map(Ticket::id).toList(),
                reservedUntil);

        return ticketRepository.saveAll(reservedTickets)
                .then(saveTransitions(reservedTickets, TicketStatus.AVAILABLE, TicketStatus.RESERVED, orderIdStr))
                .then(updateEventCounters(command.eventId(), command.quantity()))
                .then(orderRepository.save(reservedOrder));
    }

    private Mono<Void> updateEventCounters(com.tickets.domain.model.event.EventId eventId,
                                            int quantity) {
        return eventRepository.findById(eventId)
                .flatMap(event -> {
                    com.tickets.domain.model.event.Event updated = event.reserve(quantity);
                    return eventRepository.save(updated);
                })
                .then();
    }

    private Mono<Void> saveTransitions(List<Ticket> tickets, TicketStatus from, TicketStatus to,
                                       String orderId) {
        List<TicketTransition> transitions = tickets.stream()
                .map(ticket -> TicketTransition.create(ticket.id(), from, to, orderId))
                .toList();
        return transitionRepository.saveAll(transitions);
    }
}
