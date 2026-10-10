package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adapter that implements the {@link EventRepository} port by delegating
 * to {@link EventDynamoDbRepository}.
 */
@Component
public class EventPersistenceAdapter implements EventRepository {

    private final EventDynamoDbRepository dynamoDbRepository;

    public EventPersistenceAdapter(EventDynamoDbRepository dynamoDbRepository) {
        this.dynamoDbRepository = dynamoDbRepository;
    }

    @Override
    public Mono<Event> save(Event event) {
        return dynamoDbRepository.save(event);
    }

    @Override
    public Mono<Event> findById(EventId id) {
        return dynamoDbRepository.findById(id);
    }

    @Override
    public Flux<Event> findAll() {
        return dynamoDbRepository.findAll();
    }

    @Override
    public Mono<Boolean> existsByName(String name) {
        return dynamoDbRepository.existsByName(name);
    }
}
