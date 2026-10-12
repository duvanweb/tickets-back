package com.tickets.application.usecase.order;

import com.tickets.application.port.in.order.GrantComplimentaryUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.model.event.Event;
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
 * updates event counters (available−N, complimentary+N), and creates a
 * COMPLIMENTARY order.</p>
 */
public final class GrantComplimentaryService implements GrantComplimentaryUseCase {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketTransitionRepository transitionRepository;

    public GrantComplimentaryService(TicketRepository ticketRepository,
                                     EventRepository eventRepository,
                                     OrderRepository orderRepository,
                                     TicketTransitionRepository transitionRepository) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.transitionRepository = transitionRepository;
    }

    @Override
    public Mono<Order> grant(GrantComplimentaryCommand command) {
        return ticketRepository.findAvailableByEvent(command.eventId(), command.quantity())
                .collectList()
                .flatMap(availableTickets -> {
                    if (availableTickets.size() < command.quantity()) {
                        return Mono.error(new InvalidEventException(
                                "Not enough available tickets for complimentary grant: requested "
                                        + command.quantity() + ", available " + availableTickets.size()));
                    }
                    return grantComplimentary(command, availableTickets);
                });
    }

    private Mono<Order> grantComplimentary(GrantComplimentaryCommand command,
                                           List<Ticket> availableTickets) {
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

        List<TicketTransition> transitions = selectedTickets.stream()
                .map(ticket -> TicketTransition.create(ticket.id(),
                        TicketStatus.AVAILABLE, TicketStatus.COMPLIMENTARY, orderIdStr))
                .toList();

        return ticketRepository.saveAll(complimentaryTickets)
                .then(transitionRepository.saveAll(transitions))
                .then(updateEventCounters(command.eventId(), command.quantity()))
                .then(orderRepository.save(complimentaryOrder));
    }

    private Mono<Void> updateEventCounters(com.tickets.domain.model.event.EventId eventId,
                                            int quantity) {
        return eventRepository.findById(eventId)
                .flatMap(event -> {
                    Event updated = event.grantComplimentary(quantity);
                    return eventRepository.save(updated);
                })
                .then();
    }
}
