package com.tickets.application.port.in.order;

import com.tickets.domain.model.order.Order;
import com.tickets.application.usecase.order.CreateOrderCommand;
import reactor.core.publisher.Mono;

/**
 * Input port (use case interface) for creating a new order.
 */
public interface CreateOrderUseCase {

    Mono<Order> create(CreateOrderCommand command);
}
