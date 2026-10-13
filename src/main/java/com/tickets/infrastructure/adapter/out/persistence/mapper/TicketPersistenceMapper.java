package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketEntity;

import java.time.LocalDateTime;

/**
 * Manual mapper between the domain {@link Ticket} and the persistence {@link TicketEntity}.
 */
public final class TicketPersistenceMapper {

    private TicketPersistenceMapper() {
    }

    /**
     * Converts a domain {@link Ticket} to a persistence {@link TicketEntity}.
     *
     * @param ticket the domain ticket
     * @return the DynamoDB entity
     */
    public static TicketEntity toEntity(Ticket ticket) {
        TicketEntity entity = new TicketEntity();
        entity.setTicketId(ticket.id().value().toString());
        entity.setEventId(ticket.eventId().value().toString());
        entity.setStatus(ticket.status().name());
        entity.setSeatNumber(ticket.seatNumber());
        entity.setOrderId(ticket.orderId());
        entity.setCustomerId(ticket.customerId());
        entity.setReservedUntil(ticket.reservedUntil() != null ? ticket.reservedUntil().toString() : null);
        entity.setVersion(ticket.version());
        return entity;
    }

    /**
     * Converts a persistence {@link TicketEntity} to a domain {@link Ticket}.
     *
     * @param entity the DynamoDB entity
     * @return the domain ticket
     */
    public static Ticket toDomain(TicketEntity entity) {
        return Ticket.reconstitute(
                TicketId.of(entity.getTicketId()),
                EventId.of(entity.getEventId()),
                TicketStatus.valueOf(entity.getStatus()),
                entity.getSeatNumber(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getReservedUntil() != null ? LocalDateTime.parse(entity.getReservedUntil()) : null,
                entity.getVersion()
        );
    }
}
