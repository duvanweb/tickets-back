package com.tickets.application.usecase.event;

import com.tickets.application.port.in.event.ListEventsUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.model.event.Event;
import reactor.core.publisher.Flux;

/**
 * Implementation of {@link ListEventsUseCase}.
 *
 * <p>Returns all events from the {@link EventRepository} port.</p>
 */
public final class ListEventsService implements ListEventsUseCase {

    private final EventRepository repository;

    public ListEventsService(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Flux<Event> getAll() {
        return repository.findAll();
    }
}
