package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.infrastructure.adapter.out.persistence.entity.OrderEntity;
import com.tickets.infrastructure.adapter.out.persistence.mapper.OrderPersistenceMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/**
 * DynamoDB operations for {@link Order} using the Enhanced Client.
 *
 * <p>All blocking DynamoDB calls are wrapped in {@code Mono.fromCallable}
 * and subscribed on {@code Schedulers.boundedElastic()} to avoid blocking
 * the Netty event loop.</p>
 */
@Component
public class OrderDynamoDbRepository {

    private final DynamoDbTable<OrderEntity> orderTable;

    public OrderDynamoDbRepository(DynamoDbTable<OrderEntity> orderTable) {
        this.orderTable = orderTable;
    }

    /**
     * Saves (puts) an order into DynamoDB.
     *
     * @param order the domain order to persist
     * @return a {@link Mono} emitting the saved order
     */
    public Mono<Order> save(Order order) {
        return Mono.fromCallable(() -> {
                    orderTable.putItem(OrderPersistenceMapper.toEntity(order));
                    return order;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Finds an order by its ID.
     *
     * @param id the order ID
     * @return a {@link Mono} emitting the order, or empty if not found
     */
    public Mono<Order> findById(OrderId id) {
        return Mono.fromCallable(() -> {
                    OrderEntity entity = orderTable.getItem(Key.builder()
                            .partitionValue(id.value().toString())
                            .build());
                    return entity != null ? OrderPersistenceMapper.toDomain(entity) : null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Finds all orders for a given event using the {@code event-index} GSI.
     *
     * @param eventId the event ID
     * @return a {@link Flux} emitting all orders for the event
     */
    public Flux<Order> findByEventId(EventId eventId) {
        return Mono.fromCallable(() -> {
                    DynamoDbIndex<OrderEntity> index =
                            orderTable.index("event-index");
                    QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                            .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                    .partitionValue(eventId.value().toString())
                                    .build()))
                            .build();

                    return index.query(queryRequest)
                            .stream()
                            .flatMap(page -> page.items().stream())
                            .map(OrderPersistenceMapper::toDomain)
                            .toList();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }
}
