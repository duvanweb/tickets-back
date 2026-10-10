package com.tickets.domain.model.event;

import java.util.UUID;

/**
 * Value object representing the unique identifier of an {@link Event}.
 *
 * <p>Immutable record wrapping a {@link UUID}.</p>
 */
public record EventId(UUID value) {

    /**
     * Generates a new random {@link EventId}.
     *
     * @return a new {@link EventId} with a randomly generated UUID
     */
    public static EventId generate() {
        return new EventId(UUID.randomUUID());
    }

    /**
     * Creates an {@link EventId} from a string representation of a UUID.
     *
     * @param value the UUID string
     * @return a new {@link EventId}
     */
    public static EventId of(String value) {
        return new EventId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
