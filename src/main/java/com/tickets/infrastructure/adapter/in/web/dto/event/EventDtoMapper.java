package com.tickets.infrastructure.adapter.in.web.dto.event;

import com.tickets.application.usecase.event.CreateEventCommand;
import com.tickets.domain.model.event.Event;

/**
 * Manual mapper between web DTOs and application/domain objects.
 */
public final class EventDtoMapper {

    private EventDtoMapper() {
    }

    /**
     * Converts a {@link CreateEventRequest} to a {@link CreateEventCommand}.
     *
     * @param request the web request
     * @return the application command
     */
    public static CreateEventCommand toCommand(CreateEventRequest request) {
        return new CreateEventCommand(
                request.name(),
                request.date(),
                request.place(),
                request.totalCapacity());
    }

    /**
     * Converts a domain {@link Event} to an {@link EventResponse}.
     *
     * @param event the domain event
     * @return the web response
     */
    public static EventResponse toResponse(Event event) {
        return new EventResponse(
                event.id().value().toString(),
                event.name(),
                event.date(),
                event.place(),
                event.totalCapacity(),
                event.available(),
                event.reserved(),
                event.sold(),
                event.complimentary());
    }
}
