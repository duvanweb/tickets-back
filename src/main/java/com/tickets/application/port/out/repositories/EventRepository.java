package com.tickets.application.port.out.repositories;

import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Secondary port (repository interface) for event persistence.
 */
public interface EventRepository {

    Mono<Event> save(Event event);

    Mono<Event> findById(EventId id);

    Flux<Event> findAll();

    Mono<Boolean> existsByName(String name);
}
