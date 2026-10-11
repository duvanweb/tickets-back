package com.tickets.domain.model.ticket;

import com.tickets.domain.exception.InvalidTicketException;
import com.tickets.domain.model.event.EventId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    private static final EventId EVENT_ID = EventId.generate();
    private static final String SEAT_NUMBER = "A-12";

    @Test
    void create_withValidInputs_returnsAvailableTicketWithGeneratedId() {
        Ticket ticket = Ticket.create(EVENT_ID, SEAT_NUMBER);

        assertThat(ticket.id()).isNotNull();
        assertThat(ticket.eventId()).isEqualTo(EVENT_ID);
        assertThat(ticket.status()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(ticket.seatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(ticket.orderId()).isNull();
        assertThat(ticket.customerId()).isNull();
        assertThat(ticket.reservedUntil()).isNull();
        assertThat(ticket.version()).isZero();
    }

    @Test
    void create_withNullEventId_throwsInvalidTicketException() {
        assertThatThrownBy(() -> Ticket.create(null, SEAT_NUMBER))
                .isInstanceOf(InvalidTicketException.class)
                .hasMessageContaining("Event ID");
    }

    @Test
    void create_withBlankSeatNumber_throwsInvalidTicketException() {
        assertThatThrownBy(() -> Ticket.create(EVENT_ID, ""))
                .isInstanceOf(InvalidTicketException.class)
                .hasMessageContaining("Seat number");
    }

    @Test
    void create_withNullSeatNumber_throwsInvalidTicketException() {
        assertThatThrownBy(() -> Ticket.create(EVENT_ID, null))
                .isInstanceOf(InvalidTicketException.class)
                .hasMessageContaining("Seat number");
    }

    @Test
    void reconstitute_rebuildsTicketWithGivenId() {
        TicketId id = TicketId.generate();
        LocalDateTime reservedUntil = LocalDateTime.of(2026, 12, 15, 19, 45);

        Ticket ticket = Ticket.reconstitute(id, EVENT_ID, TicketStatus.RESERVED, SEAT_NUMBER,
                "order-123", "customer-456", reservedUntil, 3);

        assertThat(ticket.id()).isEqualTo(id);
        assertThat(ticket.eventId()).isEqualTo(EVENT_ID);
        assertThat(ticket.status()).isEqualTo(TicketStatus.RESERVED);
        assertThat(ticket.seatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(ticket.orderId()).isEqualTo("order-123");
        assertThat(ticket.customerId()).isEqualTo("customer-456");
        assertThat(ticket.reservedUntil()).isEqualTo(reservedUntil);
        assertThat(ticket.version()).isEqualTo(3);
    }

    @Test
    void canTransitionTo_delegatesToStatus() {
        Ticket available = Ticket.create(EVENT_ID, SEAT_NUMBER);

        assertThat(available.canTransitionTo(TicketStatus.RESERVED)).isTrue();
        assertThat(available.canTransitionTo(TicketStatus.SOLD)).isFalse();
    }

    @Test
    void transitionTo_withValidTransition_returnsNewTicketWithIncrementedVersion() {
        Ticket available = Ticket.create(EVENT_ID, SEAT_NUMBER);

        Ticket reserved = available.transitionTo(TicketStatus.RESERVED);

        assertThat(reserved.status()).isEqualTo(TicketStatus.RESERVED);
        assertThat(reserved.version()).isEqualTo(1);
        assertThat(reserved.id()).isEqualTo(available.id());
        assertThat(available.status()).isEqualTo(TicketStatus.AVAILABLE);
    }

    @Test
    void transitionTo_withInvalidTransition_throwsInvalidTicketException() {
        Ticket available = Ticket.create(EVENT_ID, SEAT_NUMBER);

        assertThatThrownBy(() -> available.transitionTo(TicketStatus.SOLD))
                .isInstanceOf(InvalidTicketException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void reserve_withAvailableTicket_returnsReservedTicketWithAssociations() {
        Ticket available = Ticket.create(EVENT_ID, SEAT_NUMBER);
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(10);

        Ticket reserved = available.reserve("order-123", "customer-456", reservedUntil);

        assertThat(reserved.status()).isEqualTo(TicketStatus.RESERVED);
        assertThat(reserved.orderId()).isEqualTo("order-123");
        assertThat(reserved.customerId()).isEqualTo("customer-456");
        assertThat(reserved.reservedUntil()).isEqualTo(reservedUntil);
        assertThat(reserved.version()).isEqualTo(1);
        assertThat(reserved.id()).isEqualTo(available.id());
        assertThat(available.status()).isEqualTo(TicketStatus.AVAILABLE);
    }

    @Test
    void reserve_withNonAvailableTicket_throwsInvalidTicketException() {
        TicketId id = TicketId.generate();
        Ticket reserved = Ticket.reconstitute(id, EVENT_ID, TicketStatus.RESERVED, SEAT_NUMBER,
                "order-1", "cust-1", null, 1);

        assertThatThrownBy(() -> reserved.reserve("order-2", "cust-2", LocalDateTime.now()))
                .isInstanceOf(InvalidTicketException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void confirm_withReservedTicket_returnsSoldTicket() {
        TicketId id = TicketId.generate();
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(10);
        Ticket reserved = Ticket.reconstitute(id, EVENT_ID, TicketStatus.RESERVED, SEAT_NUMBER,
                "order-123", "customer-456", reservedUntil, 1);

        Ticket sold = reserved.confirm();

        assertThat(sold.status()).isEqualTo(TicketStatus.SOLD);
        assertThat(sold.id()).isEqualTo(id);
        assertThat(sold.orderId()).isEqualTo("order-123");
        assertThat(sold.customerId()).isEqualTo("customer-456");
        assertThat(sold.reservedUntil()).isNull();
        assertThat(sold.version()).isEqualTo(3);
    }

    @Test
    void confirm_withNonReservedTicket_throwsInvalidTicketException() {
        Ticket available = Ticket.create(EVENT_ID, SEAT_NUMBER);

        assertThatThrownBy(available::confirm)
                .isInstanceOf(InvalidTicketException.class)
                .hasMessageContaining("Cannot confirm");
    }

    @Test
    void ticketsWithSameId_areEqual() {
        TicketId id = TicketId.generate();
        Ticket ticket1 = Ticket.reconstitute(id, EVENT_ID, TicketStatus.AVAILABLE, "A1",
                null, null, null, 0);
        Ticket ticket2 = Ticket.reconstitute(id, EVENT_ID, TicketStatus.SOLD, "B2",
                "order-1", "cust-1", null, 5);

        assertThat(ticket1).isEqualTo(ticket2);
        assertThat(ticket1.hashCode()).isEqualTo(ticket2.hashCode());
    }

    @Test
    void ticketsWithDifferentId_areNotEqual() {
        Ticket ticket1 = Ticket.create(EVENT_ID, SEAT_NUMBER);
        Ticket ticket2 = Ticket.create(EVENT_ID, SEAT_NUMBER);

        assertThat(ticket1).isNotEqualTo(ticket2);
    }
}
