package com.tickets.domain.model.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTransitionIdTest {

    @Test
    void generate_producesNonNullId() {
        TicketTransitionId id = TicketTransitionId.generate();

        assertThat(id).isNotNull();
        assertThat(id.value()).isNotNull();
    }

    @Test
    void of_parsesValidUuidString() {
        TicketTransitionId original = TicketTransitionId.generate();
        String str = original.value().toString();

        TicketTransitionId parsed = TicketTransitionId.of(str);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void toString_returnsUuidString() {
        TicketTransitionId id = TicketTransitionId.generate();

        assertThat(id.toString()).isEqualTo(id.value().toString());
    }

    @Test
    void idsWithSameValue_areEqual() {
        TicketTransitionId id1 = TicketTransitionId.generate();
        TicketTransitionId id2 = TicketTransitionId.of(id1.value().toString());

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void idsWithDifferentValues_areNotEqual() {
        TicketTransitionId id1 = TicketTransitionId.generate();
        TicketTransitionId id2 = TicketTransitionId.generate();

        assertThat(id1).isNotEqualTo(id2);
    }
}
