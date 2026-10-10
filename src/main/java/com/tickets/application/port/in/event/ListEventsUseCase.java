package com.tickets.application.port.in.event;

import com.tickets.domain.model.event.Event;
import reactor.core.publisher.Flux;

/**
 * Primary port (use case interface) for listing all events.
 */
public interface ListEventsUseCase {

    Flux<Event> getAll();
}
