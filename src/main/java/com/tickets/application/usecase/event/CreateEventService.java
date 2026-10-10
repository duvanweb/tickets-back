package com.tickets.application.usecase.event;

import com.tickets.application.port.in.event.CreateEventUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.model.event.Event;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link CreateEventUseCase}.
 *
 * <p>Creates a new {@link Event} via the domain factory, checks that no
 * event with the same name already exists, and persists it through the
 * {@link EventRepository} port.</p>
 */
public final class CreateEventService implements CreateEventUseCase {

    private final EventRepository repository;

    public CreateEventService(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Event> create(CreateEventCommand command) {
        return Mono.fromCallable(() ->
                Event.create(command.name(), command.date(), command.place(), command.totalCapacity()))
                .flatMap(event -> repository.existsByName(event.name())
                        .flatMap(exists -> exists
                                ? Mono.error(new InvalidEventException(
                                        "Event with name '" + event.name() + "' already exists"))
                                : repository.save(event)));
    }
}
