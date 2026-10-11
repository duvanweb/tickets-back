package com.tickets.infrastructure.adapter.in.web.dto.event;

import com.tickets.application.usecase.event.CreateEventCommand;
import com.tickets.application.usecase.event.EventAvailability;
import com.tickets.application.usecase.order.GrantComplimentaryCommand;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventDtoMapperTest {

    private static final String NAME = "Concierto Rock";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 12, 15, 19, 30);
    private static final String PLACE = "Estadio Nacional";
    private static final int TOTAL_CAPACITY = 5000;

    @Test
    void toCommand_mapsAllFields() {
        CreateEventRequest request = new CreateEventRequest(NAME, DATE, PLACE, TOTAL_CAPACITY);

        CreateEventCommand command = EventDtoMapper.toCommand(request);

        assertThat(command.name()).isEqualTo(NAME);
        assertThat(command.date()).isEqualTo(DATE);
        assertThat(command.place()).isEqualTo(PLACE);
        assertThat(command.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
    }

    @Test
    void toResponse_mapsAllFieldsIncludingCounters() {
        EventId id = EventId.generate();
        Event event = Event.reconstitute(id, NAME, DATE, PLACE, TOTAL_CAPACITY,
                3000, 1000, 800, 200);

        EventResponse response = EventDtoMapper.toResponse(event);

        assertThat(response.id()).isEqualTo(id.value().toString());
        assertThat(response.name()).isEqualTo(NAME);
        assertThat(response.date()).isEqualTo(DATE);
        assertThat(response.place()).isEqualTo(PLACE);
        assertThat(response.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
        assertThat(response.available()).isEqualTo(3000);
        assertThat(response.reserved()).isEqualTo(1000);
        assertThat(response.sold()).isEqualTo(800);
        assertThat(response.complimentary()).isEqualTo(200);
    }

    @Test
    void toResponse_forNewEvent_mapsInitializedCounters() {
        Event event = Event.create(NAME, DATE, PLACE, TOTAL_CAPACITY);

        EventResponse response = EventDtoMapper.toResponse(event);

        assertThat(response.available()).isEqualTo(TOTAL_CAPACITY);
        assertThat(response.reserved()).isZero();
        assertThat(response.sold()).isZero();
        assertThat(response.complimentary()).isZero();
    }

    @Test
    void toAvailabilityResponse_mapsAllFields() {
        EventId id = EventId.generate();
        EventAvailability availability = new EventAvailability(id, 5000, 3000, 1000, 800, 200);

        AvailabilityResponse response = EventDtoMapper.toAvailabilityResponse(availability);

        assertThat(response.eventId()).isEqualTo(id.value().toString());
        assertThat(response.totalCapacity()).isEqualTo(5000);
        assertThat(response.available()).isEqualTo(3000);
        assertThat(response.reserved()).isEqualTo(1000);
        assertThat(response.sold()).isEqualTo(800);
        assertThat(response.complimentary()).isEqualTo(200);
    }

    @Test
    void toGrantCommand_mapsEventIdAndRequestFields() {
        EventId eventId = EventId.generate();
        String eventIdStr = eventId.value().toString();
        GrantComplimentaryRequest request = new GrantComplimentaryRequest("customer-123", 3);

        GrantComplimentaryCommand command = EventDtoMapper.toGrantCommand(eventIdStr, request);

        assertThat(command.eventId()).isEqualTo(eventId);
        assertThat(command.customerId()).isEqualTo("customer-123");
        assertThat(command.quantity()).isEqualTo(3);
    }
}
