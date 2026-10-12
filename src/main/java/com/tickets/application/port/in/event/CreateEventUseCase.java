package com.tickets.application.port.in.event;

import com.tickets.domain.model.event.Event;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Primary port (use case interface) for creating an event.
 */
public interface CreateEventUseCase {

    /**
     * Input command for creating an event.
     *
     * @param name         the event name
     * @param date         the event date and time
     * @param place        the event location
     * @param totalCapacity the total ticket capacity
     */
    record CreateEventCommand(String name, LocalDateTime date, String place, int totalCapacity) {}

    Mono<Event> create(CreateEventCommand command);
}
