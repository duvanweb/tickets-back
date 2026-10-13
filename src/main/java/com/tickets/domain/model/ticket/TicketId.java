package com.tickets.domain.model.ticket;

import java.util.UUID;

/**
 * Value object representing the unique identifier of a {@link Ticket}.
 *
 * <p>Immutable record wrapping a {@link UUID}.</p>
 */
public record TicketId(UUID value) {

    /**
     * Generates a new random {@link TicketId}.
     *
     * @return a new {@link TicketId} with a randomly generated UUID
     */
    public static TicketId generate() {
        return new TicketId(UUID.randomUUID());
    }

    /**
     * Creates a {@link TicketId} from a string representation of a UUID.
     *
     * @param value the UUID string
     * @return a new {@link TicketId}
     */
    public static TicketId of(String value) {
        return new TicketId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
