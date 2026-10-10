package com.tickets.application.port.in.event;

import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import reactor.core.publisher.Mono;

/**
 * Primary port (use case interface) for retrieving an event by its ID.
 */
public interface GetEventUseCase {

    Mono<Event> getById(EventId id);
}
