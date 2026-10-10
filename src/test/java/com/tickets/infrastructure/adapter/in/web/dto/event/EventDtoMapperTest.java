package com.tickets.infrastructure.adapter.in.web.dto.event;

import com.tickets.application.usecase.event.CreateEventCommand;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

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
    void toResponse_mapsAllFields() {
        EventId id = EventId.generate();
        Event event = Event.reconstitute(id, NAME, DATE, PLACE, TOTAL_CAPACITY);

        EventResponse response = EventDtoMapper.toResponse(event);

        assertThat(response.id()).isEqualTo(id.value().toString());
        assertThat(response.name()).isEqualTo(NAME);
        assertThat(response.date()).isEqualTo(DATE);
        assertThat(response.place()).isEqualTo(PLACE);
        assertThat(response.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
    }
}
