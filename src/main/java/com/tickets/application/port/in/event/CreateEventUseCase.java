package com.tickets.application.port.in.event;

import com.tickets.application.usecase.event.CreateEventCommand;
import com.tickets.domain.model.event.Event;
import reactor.core.publisher.Mono;

/**
 * Primary port (use case interface) for creating an event.
 */
public interface CreateEventUseCase {

    Mono<Event> create(CreateEventCommand command);
}
