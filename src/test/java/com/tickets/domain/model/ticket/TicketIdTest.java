package com.tickets.domain.model.ticket;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketIdTest {

    @Test
    void generate_createsNonNullId() {
        TicketId id = TicketId.generate();

        assertThat(id).isNotNull();
        assertThat(id.value()).isNotNull();
    }

    @Test
    void of_parsesUuidString() {
        UUID uuid = UUID.randomUUID();
        TicketId id = TicketId.of(uuid.toString());

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void toString_returnsUuidString() {
        TicketId id = TicketId.generate();

        assertThat(id.toString()).isEqualTo(id.value().toString());
    }

    @Test
    void equals_andHashCode_basedOnValue() {
        UUID uuid = UUID.randomUUID();
        TicketId id1 = new TicketId(uuid);
        TicketId id2 = new TicketId(uuid);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
