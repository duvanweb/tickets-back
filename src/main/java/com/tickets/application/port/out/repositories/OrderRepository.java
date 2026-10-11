package com.tickets.application.port.out.repositories;

import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import reactor.core.publisher.Mono;

/**
 * Secondary port (repository interface) for order persistence.
 */
public interface OrderRepository {

    Mono<Order> save(Order order);

    Mono<Order> findById(OrderId id);
}
