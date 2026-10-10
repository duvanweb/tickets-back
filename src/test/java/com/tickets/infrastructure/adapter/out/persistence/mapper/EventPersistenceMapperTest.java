package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventPersistenceMapperTest {

    private static final EventId EVENT_ID = EventId.generate();
    private static final String NAME = "Concierto Rock";
    private static final LocalDate DATE = LocalDate.of(2026, 12, 15);
    private static final String PLACE = "Estadio Nacional";
    private static final int TOTAL_CAPACITY = 5000;

    @Test
    void toEntity_mapsAllFields() {
        Event event = Event.reconstitute(EVENT_ID, NAME, DATE, PLACE, TOTAL_CAPACITY);

        EventEntity entity = EventPersistenceMapper.toEntity(event);

        assertThat(entity.getEventId()).isEqualTo(EVENT_ID.value().toString());
        assertThat(entity.getName()).isEqualTo(NAME);
        assertThat(entity.getEventDate()).isEqualTo(DATE.toString());
        assertThat(entity.getPlace()).isEqualTo(PLACE);
        assertThat(entity.getTotalCapacity()).isEqualTo(TOTAL_CAPACITY);
    }

    @Test
    void toDomain_mapsAllFields() {
        EventEntity entity = new EventEntity();
        entity.setEventId(EVENT_ID.value().toString());
        entity.setName(NAME);
        entity.setEventDate(DATE.toString());
        entity.setPlace(PLACE);
        entity.setTotalCapacity(TOTAL_CAPACITY);

        Event event = EventPersistenceMapper.toDomain(entity);

        assertThat(event.id()).isEqualTo(EVENT_ID);
        assertThat(event.name()).isEqualTo(NAME);
        assertThat(event.date()).isEqualTo(DATE);
        assertThat(event.place()).isEqualTo(PLACE);
        assertThat(event.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
    }

    @Test
    void toEntityAndBack_preservesAllFields() {
        Event original = Event.reconstitute(EVENT_ID, NAME, DATE, PLACE, TOTAL_CAPACITY);

        Event roundTripped = EventPersistenceMapper.toDomain(EventPersistenceMapper.toEntity(original));

        assertThat(roundTripped).isEqualTo(original);
        assertThat(roundTripped.name()).isEqualTo(NAME);
        assertThat(roundTripped.date()).isEqualTo(DATE);
        assertThat(roundTripped.place()).isEqualTo(PLACE);
        assertThat(roundTripped.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
    }
}
