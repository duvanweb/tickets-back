package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventPersistenceMapperTest {

    private static final EventId EVENT_ID = EventId.generate();
    private static final String NAME = "Concierto Rock";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 12, 15, 19, 30);
    private static final String PLACE = "Estadio Nacional";
    private static final int TOTAL_CAPACITY = 5000;
    private static final int AVAILABLE = 3000;
    private static final int RESERVED = 1000;
    private static final int SOLD = 800;
    private static final int COMPLIMENTARY = 200;

    @Test
    void toEntity_mapsAllFields() {
        Event event = Event.reconstitute(EVENT_ID, NAME, DATE, PLACE, TOTAL_CAPACITY,
                AVAILABLE, RESERVED, SOLD, COMPLIMENTARY);

        EventEntity entity = EventPersistenceMapper.toEntity(event);

        assertThat(entity.getEventId()).isEqualTo(EVENT_ID.value().toString());
        assertThat(entity.getName()).isEqualTo(NAME);
        assertThat(entity.getEventDate()).isEqualTo(DATE.toString());
        assertThat(entity.getPlace()).isEqualTo(PLACE);
        assertThat(entity.getTotalCapacity()).isEqualTo(TOTAL_CAPACITY);
        assertThat(entity.getAvailable()).isEqualTo(AVAILABLE);
        assertThat(entity.getReserved()).isEqualTo(RESERVED);
        assertThat(entity.getSold()).isEqualTo(SOLD);
        assertThat(entity.getComplimentary()).isEqualTo(COMPLIMENTARY);
    }

    @Test
    void toDomain_mapsAllFields() {
        EventEntity entity = new EventEntity();
        entity.setEventId(EVENT_ID.value().toString());
        entity.setName(NAME);
        entity.setEventDate(DATE.toString());
        entity.setPlace(PLACE);
        entity.setTotalCapacity(TOTAL_CAPACITY);
        entity.setAvailable(AVAILABLE);
        entity.setReserved(RESERVED);
        entity.setSold(SOLD);
        entity.setComplimentary(COMPLIMENTARY);

        Event event = EventPersistenceMapper.toDomain(entity);

        assertThat(event.id()).isEqualTo(EVENT_ID);
        assertThat(event.name()).isEqualTo(NAME);
        assertThat(event.date()).isEqualTo(DATE);
        assertThat(event.place()).isEqualTo(PLACE);
        assertThat(event.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
        assertThat(event.available()).isEqualTo(AVAILABLE);
        assertThat(event.reserved()).isEqualTo(RESERVED);
        assertThat(event.sold()).isEqualTo(SOLD);
        assertThat(event.complimentary()).isEqualTo(COMPLIMENTARY);
    }

    @Test
    void toEntityAndBack_preservesAllFields() {
        Event original = Event.reconstitute(EVENT_ID, NAME, DATE, PLACE, TOTAL_CAPACITY,
                AVAILABLE, RESERVED, SOLD, COMPLIMENTARY);

        Event roundTripped = EventPersistenceMapper.toDomain(EventPersistenceMapper.toEntity(original));

        assertThat(roundTripped).isEqualTo(original);
        assertThat(roundTripped.name()).isEqualTo(NAME);
        assertThat(roundTripped.date()).isEqualTo(DATE);
        assertThat(roundTripped.place()).isEqualTo(PLACE);
        assertThat(roundTripped.totalCapacity()).isEqualTo(TOTAL_CAPACITY);
        assertThat(roundTripped.available()).isEqualTo(AVAILABLE);
        assertThat(roundTripped.reserved()).isEqualTo(RESERVED);
        assertThat(roundTripped.sold()).isEqualTo(SOLD);
        assertThat(roundTripped.complimentary()).isEqualTo(COMPLIMENTARY);
    }
}
