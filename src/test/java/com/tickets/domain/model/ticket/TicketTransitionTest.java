package com.tickets.domain.model.ticket;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTransitionTest {

    private static final TicketId TICKET_ID = TicketId.generate();
    private static final String ORDER_ID = "order-123";

    @Test
    void create_withValidInputs_returnsTransitionWithGeneratedIdAndTimestamp() {
        TicketTransition transition = TicketTransition.create(
                TICKET_ID, TicketStatus.AVAILABLE, TicketStatus.RESERVED, ORDER_ID);

        assertThat(transition.id()).isNotNull();
        assertThat(transition.ticketId()).isEqualTo(TICKET_ID);
        assertThat(transition.fromStatus()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(transition.toStatus()).isEqualTo(TicketStatus.RESERVED);
        assertThat(transition.orderId()).isEqualTo(ORDER_ID);
        assertThat(transition.timestamp()).isNotNull();
    }

    @Test
    void create_withNullTicketId_throwsNullPointerException() {
        assertThatThrownBy(() -> TicketTransition.create(
                null, TicketStatus.AVAILABLE, TicketStatus.RESERVED, ORDER_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Ticket ID");
    }

    @Test
    void create_withNullFromStatus_throwsNullPointerException() {
        assertThatThrownBy(() -> TicketTransition.create(
                TICKET_ID, null, TicketStatus.RESERVED, ORDER_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("From status");
    }

    @Test
    void create_withNullToStatus_throwsNullPointerException() {
        assertThatThrownBy(() -> TicketTransition.create(
                TICKET_ID, TicketStatus.AVAILABLE, null, ORDER_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("To status");
    }

    @Test
    void create_withNullOrderId_works() {
        TicketTransition transition = TicketTransition.create(
                TICKET_ID, TicketStatus.RESERVED, TicketStatus.AVAILABLE, null);

        assertThat(transition.orderId()).isNull();
    }

    @Test
    void reconstitute_rebuildsTransitionWithGivenValues() {
        TicketTransitionId id = TicketTransitionId.generate();
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 11, 15, 30);

        TicketTransition transition = TicketTransition.reconstitute(
                id, TICKET_ID, TicketStatus.RESERVED, TicketStatus.SOLD, ORDER_ID, timestamp);

        assertThat(transition.id()).isEqualTo(id);
        assertThat(transition.ticketId()).isEqualTo(TICKET_ID);
        assertThat(transition.fromStatus()).isEqualTo(TicketStatus.RESERVED);
        assertThat(transition.toStatus()).isEqualTo(TicketStatus.SOLD);
        assertThat(transition.orderId()).isEqualTo(ORDER_ID);
        assertThat(transition.timestamp()).isEqualTo(timestamp);
    }

    @Test
    void transitionsWithSameId_areEqual() {
        TicketTransitionId id = TicketTransitionId.generate();
        LocalDateTime ts = LocalDateTime.now();

        TicketTransition t1 = TicketTransition.reconstitute(
                id, TICKET_ID, TicketStatus.AVAILABLE, TicketStatus.RESERVED, "order-1", ts);
        TicketTransition t2 = TicketTransition.reconstitute(
                id, TicketId.generate(), TicketStatus.SOLD, TicketStatus.COMPLIMENTARY, "order-2", ts.plusDays(1));

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    void transitionsWithDifferentId_areNotEqual() {
        TicketTransition t1 = TicketTransition.create(
                TICKET_ID, TicketStatus.AVAILABLE, TicketStatus.RESERVED, ORDER_ID);
        TicketTransition t2 = TicketTransition.create(
                TICKET_ID, TicketStatus.AVAILABLE, TicketStatus.RESERVED, ORDER_ID);

        assertThat(t1).isNotEqualTo(t2);
    }
}
