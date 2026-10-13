package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPersistenceMapperTest {

    private static final TicketId TICKET_ID = TicketId.generate();
    private static final EventId EVENT_ID = EventId.generate();
    private static final TicketStatus STATUS = TicketStatus.RESERVED;
    private static final String SEAT_NUMBER = "A-12";
    private static final String ORDER_ID = "order-123";
    private static final String CUSTOMER_ID = "customer-456";
    private static final LocalDateTime RESERVED_UNTIL = LocalDateTime.of(2026, 12, 15, 19, 45);
    private static final long VERSION = 3;

    @Test
    void toEntity_mapsAllFields() {
        Ticket ticket = Ticket.reconstitute(TICKET_ID, EVENT_ID, STATUS, SEAT_NUMBER,
                ORDER_ID, CUSTOMER_ID, RESERVED_UNTIL, VERSION);

        TicketEntity entity = TicketPersistenceMapper.toEntity(ticket);

        assertThat(entity.getTicketId()).isEqualTo(TICKET_ID.value().toString());
        assertThat(entity.getEventId()).isEqualTo(EVENT_ID.value().toString());
        assertThat(entity.getStatus()).isEqualTo(STATUS.name());
        assertThat(entity.getSeatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(entity.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(entity.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(entity.getReservedUntil()).isEqualTo(RESERVED_UNTIL.toString());
        assertThat(entity.getVersion()).isEqualTo(VERSION);
    }

    @Test
    void toEntity_withNullOptionalFields_mapsToNull() {
        Ticket ticket = Ticket.create(EVENT_ID, SEAT_NUMBER);

        TicketEntity entity = TicketPersistenceMapper.toEntity(ticket);

        assertThat(entity.getOrderId()).isNull();
        assertThat(entity.getCustomerId()).isNull();
        assertThat(entity.getReservedUntil()).isNull();
    }

    @Test
    void toDomain_mapsAllFields() {
        TicketEntity entity = new TicketEntity();
        entity.setTicketId(TICKET_ID.value().toString());
        entity.setEventId(EVENT_ID.value().toString());
        entity.setStatus(STATUS.name());
        entity.setSeatNumber(SEAT_NUMBER);
        entity.setOrderId(ORDER_ID);
        entity.setCustomerId(CUSTOMER_ID);
        entity.setReservedUntil(RESERVED_UNTIL.toString());
        entity.setVersion(VERSION);

        Ticket ticket = TicketPersistenceMapper.toDomain(entity);

        assertThat(ticket.id()).isEqualTo(TICKET_ID);
        assertThat(ticket.eventId()).isEqualTo(EVENT_ID);
        assertThat(ticket.status()).isEqualTo(STATUS);
        assertThat(ticket.seatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(ticket.orderId()).isEqualTo(ORDER_ID);
        assertThat(ticket.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(ticket.reservedUntil()).isEqualTo(RESERVED_UNTIL);
        assertThat(ticket.version()).isEqualTo(VERSION);
    }

    @Test
    void toDomain_withNullOptionalFields_mapsToNull() {
        TicketEntity entity = new TicketEntity();
        entity.setTicketId(TICKET_ID.value().toString());
        entity.setEventId(EVENT_ID.value().toString());
        entity.setStatus(TicketStatus.AVAILABLE.name());
        entity.setSeatNumber(SEAT_NUMBER);
        entity.setVersion(0);

        Ticket ticket = TicketPersistenceMapper.toDomain(entity);

        assertThat(ticket.orderId()).isNull();
        assertThat(ticket.customerId()).isNull();
        assertThat(ticket.reservedUntil()).isNull();
    }

    @Test
    void toEntityAndBack_preservesAllFields() {
        Ticket original = Ticket.reconstitute(TICKET_ID, EVENT_ID, STATUS, SEAT_NUMBER,
                ORDER_ID, CUSTOMER_ID, RESERVED_UNTIL, VERSION);

        Ticket roundTripped = TicketPersistenceMapper.toDomain(TicketPersistenceMapper.toEntity(original));

        assertThat(roundTripped).isEqualTo(original);
        assertThat(roundTripped.eventId()).isEqualTo(EVENT_ID);
        assertThat(roundTripped.status()).isEqualTo(STATUS);
        assertThat(roundTripped.seatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(roundTripped.orderId()).isEqualTo(ORDER_ID);
        assertThat(roundTripped.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(roundTripped.reservedUntil()).isEqualTo(RESERVED_UNTIL);
        assertThat(roundTripped.version()).isEqualTo(VERSION);
    }
}
