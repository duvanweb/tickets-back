package com.tickets.domain.model.order;

import java.util.UUID;

/**
 * Value object representing the unique identifier of an {@link Order}.
 *
 * <p>Immutable record wrapping a {@link UUID}.</p>
 */
public record OrderId(UUID value) {

    /**
     * Generates a new random {@link OrderId}.
     *
     * @return a new {@link OrderId} with a randomly generated UUID
     */
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    /**
     * Creates an {@link OrderId} from a string representation of a UUID.
     *
     * @param value the UUID string
     * @return a new {@link OrderId}
     */
    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
