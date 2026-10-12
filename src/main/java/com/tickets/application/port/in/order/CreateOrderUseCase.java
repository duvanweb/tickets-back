package com.tickets.application.port.in.order;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import reactor.core.publisher.Mono;

/**
 * Input port (use case interface) for creating a new order.
 */
public interface CreateOrderUseCase {

    /**
     * Command for creating a new order.
     *
     * @param eventId   the event to buy tickets for
     * @param customerId the customer placing the order
     * @param quantity   the number of tickets requested
     */
    record CreateOrderCommand(EventId eventId, String customerId, int quantity) {}

    Mono<Order> create(CreateOrderCommand command);
}
