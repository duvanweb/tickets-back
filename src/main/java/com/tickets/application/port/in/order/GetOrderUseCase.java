package com.tickets.application.port.in.order;

import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import reactor.core.publisher.Mono;

/**
 * Input port (use case interface) for retrieving an order by ID.
 */
public interface GetOrderUseCase {

    Mono<Order> getById(OrderId id);
}
