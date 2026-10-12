package com.tickets.application.port.in.event;

import com.tickets.application.usecase.event.model.EventAvailability;
import com.tickets.domain.model.event.EventId;
import reactor.core.publisher.Mono;

/**
 * Use case for querying real-time ticket availability for an event.
 */
public interface GetAvailabilityUseCase {

    Mono<EventAvailability> getAvailability(EventId id);
}
