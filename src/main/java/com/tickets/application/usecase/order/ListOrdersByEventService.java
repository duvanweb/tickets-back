package com.tickets.application.usecase.order;

import com.tickets.application.port.in.order.ListOrdersByEventUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link ListOrdersByEventUseCase}.
 *
 * <p>Validates that the event exists (throws {@link EventNotFoundException}
 * if not) and then queries all orders for that event through the
 * {@link OrderRepository} port.</p>
 */
public final class ListOrdersByEventService implements ListOrdersByEventUseCase {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;

    public ListOrdersByEventService(OrderRepository orderRepository,
                                    EventRepository eventRepository) {
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public Flux<Order> findByEventId(EventId eventId) {
        return eventRepository.findById(eventId)
                .switchIfEmpty(Mono.error(new EventNotFoundException(
                        "Event not found with id: " + eventId)))
                .thenMany(orderRepository.findByEventId(eventId));
    }
}
