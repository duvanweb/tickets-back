package com.tickets.infrastructure.adapter.in.web.dto.event;

import com.tickets.application.port.in.event.CreateEventUseCase.CreateEventCommand;
import com.tickets.application.usecase.event.model.EventAvailability;
import com.tickets.application.port.in.order.GrantComplimentaryUseCase.GrantComplimentaryCommand;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;

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

    /**
     * Converts an {@link EventAvailability} to an {@link AvailabilityResponse}.
     *
     * @param availability the availability read model
     * @return the web response
     */
    public static AvailabilityResponse toAvailabilityResponse(EventAvailability availability) {
        return new AvailabilityResponse(
                availability.eventId().value().toString(),
                availability.totalCapacity(),
                availability.available(),
                availability.reserved(),
                availability.sold(),
                availability.complimentary());
    }

    /**
     * Converts a path variable event ID and a {@link GrantComplimentaryRequest}
     * to a {@link GrantComplimentaryCommand}.
     *
     * @param eventIdStr the event ID string from the path
     * @param request    the web request
     * @return the application command
     */
    public static GrantComplimentaryCommand toGrantCommand(String eventIdStr,
                                                           GrantComplimentaryRequest request) {
        return new GrantComplimentaryCommand(
                EventId.of(eventIdStr),
                request.customerId(),
                request.quantity());
    }
}
