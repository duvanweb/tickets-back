package com.tickets.domain.model.order;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderIdTest {

    @Test
    void generate_createsNonNullId() {
        OrderId id = OrderId.generate();

        assertThat(id).isNotNull();
        assertThat(id.value()).isNotNull();
    }

    @Test
    void of_parsesUuidString() {
        UUID uuid = UUID.randomUUID();
        OrderId id = OrderId.of(uuid.toString());

        assertThat(id.value()).isEqualTo(uuid);
    }

    @Test
    void toString_returnsUuidString() {
        OrderId id = OrderId.generate();

        assertThat(id.toString()).isEqualTo(id.value().toString());
    }

    @Test
    void equals_andHashCode_basedOnValue() {
        UUID uuid = UUID.randomUUID();
        OrderId id1 = new OrderId(uuid);
        OrderId id2 = new OrderId(uuid);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
