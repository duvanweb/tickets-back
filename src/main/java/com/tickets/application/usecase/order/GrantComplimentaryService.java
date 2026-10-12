package com.tickets.application.usecase.order;
import com.tickets.application.port.in.order.GrantComplimentaryUseCase.GrantComplimentaryCommand;

import com.tickets.application.port.in.order.GrantComplimentaryUseCase;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketReservationRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.domain.model.ticket.TicketTransition;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Implementation of {@link GrantComplimentaryUseCase}.
 *
 * <p>Finds available tickets for the event, transitions them to COMPLIMENTARY,
 * and atomically updates event counters and ticket status using DynamoDB
 * TransactWriteItems. If the transaction fails because tickets were claimed
 * by another concurrent request, retries with a fresh set of available tickets
 * (up to {@value #MAX_RETRIES} times).</p>
 */
public final class GrantComplimentaryService implements GrantComplimentaryUseCase {

    private static final int MAX_RETRIES = 3;

    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final TicketTransitionRepository transitionRepository;
    private final TicketReservationRepository ticketReservationRepository;

    public GrantComplimentaryService(TicketRepository ticketRepository,
                                     OrderRepository orderRepository,
                                     TicketTransitionRepository transitionRepository,
                                     TicketReservationRepository ticketReservationRepository) {
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.transitionRepository = transitionRepository;
        this.ticketReservationRepository = ticketReservationRepository;
    }

    @Override
    public Mono<Order> grant(GrantComplimentaryCommand command) {
        return grantWithRetries(command, 1);
    }

    private Mono<Order> grantWithRetries(GrantComplimentaryCommand command, int attempt) {
        if (attempt > MAX_RETRIES) {
            return Mono.error(new InvalidEventException(
                    "Failed to grant complimentary tickets after " + MAX_RETRIES + " attempts"));
        }
        return ticketRepository.findAvailableByEvent(command.eventId(), command.quantity())
                .collectList()
                .flatMap(availableTickets -> {
                    if (availableTickets.size() < command.quantity()) {
                        return Mono.error(new InvalidEventException(
                                "Not enough available tickets for complimentary grant: requested "
                                        + command.quantity() + ", available " + availableTickets.size()));
                    }
                    return attemptGrant(command, availableTickets, attempt);
                });
    }

    private Mono<Order> attemptGrant(GrantComplimentaryCommand command,
                                     List<Ticket> availableTickets, int attempt) {
        List<Ticket> selectedTickets = availableTickets.stream()
                .limit(command.quantity())
                .toList();

        Order complimentaryOrder = Order.createComplimentary(
                command.eventId(), command.customerId(), command.quantity(),
                selectedTickets.stream().map(Ticket::id).toList());

        String orderIdStr = complimentaryOrder.id().value().toString();

        List<Ticket> complimentaryTickets = selectedTickets.stream()
                .map(ticket -> ticket.grantComplimentary(orderIdStr, command.customerId()))
                .toList();

        return ticketReservationRepository.grantComplimentaryAtomically(
                        command.eventId(), command.quantity(), complimentaryTickets)
                .flatMap(result -> switch (result) {
                    case ReservationResult.Success ignored ->
                            saveTransitions(selectedTickets, orderIdStr)
                                    .then(orderRepository.save(complimentaryOrder));
                    case ReservationResult.EventConditionFailed ignored ->
                            Mono.error(new InvalidEventException(
                                    "Not enough available tickets for complimentary grant"));
                    case ReservationResult.TicketConditionFailed ignored ->
                            grantWithRetries(command, attempt + 1);
                });
    }

    private Mono<Void> saveTransitions(List<Ticket> tickets, String orderId) {
        List<TicketTransition> transitions = tickets.stream()
                .map(ticket -> TicketTransition.create(ticket.id(),
                        TicketStatus.AVAILABLE, TicketStatus.COMPLIMENTARY, orderId))
                .toList();
        return transitionRepository.saveAll(transitions);
    }
}
