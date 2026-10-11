package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.application.port.out.repositories.OrderRepository;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adapter that implements the {@link OrderRepository} port by delegating
 * to {@link OrderDynamoDbRepository}.
 */
@Component
public class OrderPersistenceAdapter implements OrderRepository {

    private final OrderDynamoDbRepository dynamoDbRepository;

    public OrderPersistenceAdapter(OrderDynamoDbRepository dynamoDbRepository) {
        this.dynamoDbRepository = dynamoDbRepository;
    }

    @Override
    public Mono<Order> save(Order order) {
        return dynamoDbRepository.save(order);
    }

    @Override
    public Mono<Order> findById(OrderId id) {
        return dynamoDbRepository.findById(id);
    }
}
