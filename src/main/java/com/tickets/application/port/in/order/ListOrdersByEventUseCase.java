package com.tickets.application.port.in.order;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import reactor.core.publisher.Flux;

/**
 * Use case for listing all orders associated with a given event.
 */
public interface ListOrdersByEventUseCase {

    Flux<Order> findByEventId(EventId eventId);
}
