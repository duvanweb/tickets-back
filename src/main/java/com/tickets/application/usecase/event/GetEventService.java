package com.tickets.application.usecase.event;

import com.tickets.application.port.in.event.GetEventUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link GetEventUseCase}.
 *
 * <p>Retrieves an {@link Event} by its ID. Throws {@link EventNotFoundException}
 * if the event does not exist.</p>
 */
public final class GetEventService implements GetEventUseCase {

    private final EventRepository repository;

    public GetEventService(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Event> getById(EventId id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new EventNotFoundException(
                        "Event not found with id: " + id)));
    }
}
