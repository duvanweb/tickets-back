package com.tickets.application.usecase.order;

import com.tickets.application.port.in.order.GetOrderUseCase;
import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.domain.exception.OrderNotFoundException;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link GetOrderUseCase}.
 *
 * <p>Retrieves an {@link Order} by its ID through the {@link OrderRepository} port.
 * Throws {@link OrderNotFoundException} if the order does not exist.</p>
 */
public final class GetOrderService implements GetOrderUseCase {

    private final OrderRepository repository;

    public GetOrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Order> getById(OrderId id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(
                        "Order not found with id: " + id)));
    }
}
