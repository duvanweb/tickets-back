package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import com.tickets.infrastructure.adapter.out.persistence.mapper.EventPersistenceMapper;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;

/**
 * DynamoDB operations for {@link Event} using the Enhanced Client.
 *
 * <p>All blocking DynamoDB calls are wrapped in {@code Mono.fromCallable} /
 * {@code Flux.fromIterable} and subscribed on {@code Schedulers.boundedElastic()}
 * to avoid blocking the Netty event loop.</p>
 */
@Component
public class EventDynamoDbRepository {

    private final DynamoDbTable<EventEntity> eventTable;

    public EventDynamoDbRepository(DynamoDbTable<EventEntity> eventTable) {
        this.eventTable = eventTable;
    }

    /**
     * Saves (puts) an event into DynamoDB.
     *
     * @param event the domain event to persist
     * @return a {@link Mono} emitting the saved event
     */
    public Mono<Event> save(Event event) {
        return Mono.fromCallable(() -> {
                    eventTable.putItem(EventPersistenceMapper.toEntity(event));
                    return event;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Finds an event by its ID.
     *
     * @param id the event ID
     * @return a {@link Mono} emitting the event, or empty if not found
     */
    public Mono<Event> findById(EventId id) {
        return Mono.fromCallable(() -> {
                    EventEntity entity = eventTable.getItem(Key.builder()
                            .partitionValue(id.value().toString())
                            .build());
                    return entity != null ? EventPersistenceMapper.toDomain(entity) : null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Scans the entire events table and returns all events.
     *
     * @return a {@link Flux} emitting all events
     */
    public Flux<Event> findAll() {
        return Mono.fromCallable(() -> {
                    List<EventEntity> entities = eventTable.scan(ScanEnhancedRequest.builder().build())
                            .items()
                            .stream()
                            .toList();
                    return entities.stream()
                            .map(EventPersistenceMapper::toDomain)
                            .toList();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Checks whether an event with the given name already exists.
     *
     * <p>Queries the {@code name-index} GSI for the given name.</p>
     *
     * @param name the event name to check
     * @return a {@link Mono} emitting {@code true} if an event exists, {@code false} otherwise
     */
    public Mono<Boolean> existsByName(String name) {
        return Mono.fromCallable(() -> {
                    QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                            .queryConditional(QueryConditional.keyEqualTo(
                                    Key.builder().partitionValue(name).build()))
                            .limit(1)
                            .build();
                    return eventTable.index("name-index")
                            .query(queryRequest)
                            .stream()
                            .flatMap(page -> page.items().stream())
                            .findFirst()
                            .isPresent();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
