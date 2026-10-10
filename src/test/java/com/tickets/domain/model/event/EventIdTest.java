package com.tickets.domain.model.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventIdTest {

    @Test
    void generate_createsNonNullId() {
        EventId id = EventId.generate();

        assertThat(id).isNotNull();
        assertThat(id.value()).isNotNull();
    }

    @Test
    void of_parsesUuidString() {
        UUID uuid = UUID.randomUUID();
        EventId id = EventId.of(uuid.toString());

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void toString_returnsUuidString() {
        EventId id = EventId.generate();

        assertThat(id.toString()).isEqualTo(id.value().toString());
    }

    @Test
    void equals_andHashCode_basedOnValue() {
        UUID uuid = UUID.randomUUID();
        EventId id1 = new EventId(uuid);
        EventId id2 = new EventId(uuid);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
