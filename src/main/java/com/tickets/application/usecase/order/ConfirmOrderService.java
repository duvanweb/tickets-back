package com.tickets.application.usecase.order;

import com.tickets.application.port.in.order.ConfirmOrderUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.domain.exception.OrderNotFoundException;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.ticket.Ticket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Implementation of {@link ConfirmOrderUseCase}.
 *
 * <p>Loads the order, validates it is RESERVED and not expired, loads the
 * associated tickets from the order's {@code ticketIds}, transitions them to
 * SOLD, updates event counters ({@code reserved−N, sold+N}), and transitions
 * the order to SOLD.</p>
 */
public final class ConfirmOrderService implements ConfirmOrderUseCase {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public ConfirmOrderService(OrderRepository orderRepository,
                               TicketRepository ticketRepository,
                               EventRepository eventRepository) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<Order> confirm(OrderId id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(
                        "Order not found with id: " + id)))
                .flatMap(this::confirmOrder);
    }

    private Mono<Order> confirmOrder(Order order) {
        Order confirmed = order.confirm();

        return Flux.fromIterable(order.ticketIds())
                .flatMap(ticketRepository::findById)
                .collectList()
                .flatMap(tickets -> confirmTicketsAndSale(order, confirmed, tickets));
    }

    private Mono<Order> confirmTicketsAndSale(Order order, Order confirmedOrder,
                                              List<Ticket> tickets) {
        List<Ticket> soldTickets = tickets.stream()
                .map(Ticket::confirm)
                .toList();

        return ticketRepository.saveAll(soldTickets)
                .then(updateEventCounters(order.eventId(), order.quantity()))
                .then(orderRepository.save(confirmedOrder));
    }

    private Mono<Void> updateEventCounters(com.tickets.domain.model.event.EventId eventId,
                                            int quantity) {
        return eventRepository.findById(eventId)
                .flatMap(event -> {
                    com.tickets.domain.model.event.Event updated = event.confirmSale(quantity);
                    return eventRepository.save(updated);
                })
                .then();
    }
}
