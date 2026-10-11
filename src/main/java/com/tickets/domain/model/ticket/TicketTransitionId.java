package com.tickets.domain.model.ticket;

import java.util.UUID;

/**
 * Value object representing the unique identifier of a ticket transition audit record.
 */
public record TicketTransitionId(UUID value) {

    public static TicketTransitionId generate() {
        return new TicketTransitionId(UUID.randomUUID());
    }

    public static TicketTransitionId of(String value) {
        return new TicketTransitionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
