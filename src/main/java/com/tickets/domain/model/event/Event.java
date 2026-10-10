package com.tickets.domain.model.event;

import com.tickets.domain.exception.InvalidEventException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root representing an event.
 *
 * <p>Contains basic information (name, date, place, total capacity) and
 * inventory counters (available, reserved, sold, complimentary) that provide
 * O(1) availability reads and serve as the atomic gate against overselling.
 * Instances are created via the {@link #create} factory method, which
 * enforces all invariants and initializes counters. The {@link #reconstitute}
 * method rebuilds the aggregate from persistence without re-validating.</p>
 */
public final class Event {

    private final EventId id;
    private final String name;
    private final LocalDateTime date;
    private final String place;
    private final int totalCapacity;
    private final int available;
    private final int reserved;
    private final int sold;
    private final int complimentary;

    private Event(EventId id, String name, LocalDateTime date, String place, int totalCapacity,
                  int available, int reserved, int sold, int complimentary) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.place = place;
        this.totalCapacity = totalCapacity;
        this.available = available;
        this.reserved = reserved;
        this.sold = sold;
        this.complimentary = complimentary;
    }

    /**
     * Factory method that creates a new {@link Event} with a generated ID.
     *
     * <p>Validates that name and place are not blank, date is not null,
     * and totalCapacity is positive. Initializes inventory counters:
     * {@code available = totalCapacity}, {@code reserved = 0},
     * {@code sold = 0}, {@code complimentary = 0}.</p>
     *
     * @param name         the event name
     * @param date         the event date
     * @param place        the event location
     * @param totalCapacity the total ticket capacity
     * @return a new {@link Event}
     * @throws InvalidEventException if any validation fails
     */
    public static Event create(String name, LocalDateTime date, String place, int totalCapacity) {
        validate(name, date, place, totalCapacity);
        return new Event(EventId.generate(), name, date, place, totalCapacity,
                totalCapacity, 0, 0, 0);
    }

    /**
     * Rebuilds an {@link Event} from persistence without validation or ID generation.
     *
     * @param id           the existing event ID
     * @param name         the event name
     * @param date         the event date
     * @param place        the event location
     * @param totalCapacity the total ticket capacity
     * @param available     the available ticket count
     * @param reserved      the reserved ticket count
     * @param sold          the sold ticket count
     * @param complimentary the complimentary ticket count
     * @return a reconstituted {@link Event}
     */
    public static Event reconstitute(EventId id, String name, LocalDateTime date, String place,
                                      int totalCapacity, int available, int reserved,
                                      int sold, int complimentary) {
        return new Event(id, name, date, place, totalCapacity, available, reserved,
                sold, complimentary);
    }

    private static void validate(String name, LocalDateTime date, String place, int totalCapacity) {
        if (name == null || name.isBlank()) {
            throw new InvalidEventException("Event name must not be blank");
        }
        if (date == null) {
            throw new InvalidEventException("Event date must not be null");
        }
        if (place == null || place.isBlank()) {
            throw new InvalidEventException("Event place must not be blank");
        }
        if (totalCapacity <= 0) {
            throw new InvalidEventException("Event total capacity must be positive");
        }
    }

    /**
     * Reserves the given quantity of tickets, returning a new immutable {@link Event}
     * with adjusted counters: {@code available − quantity}, {@code reserved + quantity}.
     *
     * @param quantity the number of tickets to reserve (must be positive)
     * @return a new {@link Event} with updated counters
     * @throws InvalidEventException if quantity is invalid or insufficient availability
     */
    public Event reserve(int quantity) {
        if (quantity <= 0) {
            throw new InvalidEventException("Reservation quantity must be positive");
        }
        if (available < quantity) {
            throw new InvalidEventException(
                    "Not enough available tickets: requested " + quantity + ", available " + available);
        }
        return new Event(id, name, date, place, totalCapacity,
                available - quantity, reserved + quantity, sold, complimentary);
    }

    /**
     * Confirms the sale of the given quantity of reserved tickets, returning a new
     * immutable {@link Event} with adjusted counters: {@code reserved − quantity},
     * {@code sold + quantity}.
     *
     * @param quantity the number of tickets to sell (must be positive)
     * @return a new {@link Event} with updated counters
     * @throws InvalidEventException if quantity is invalid or insufficient reserved
     */
    public Event confirmSale(int quantity) {
        if (quantity <= 0) {
            throw new InvalidEventException("Sale quantity must be positive");
        }
        if (reserved < quantity) {
            throw new InvalidEventException(
                    "Not enough reserved tickets: requested " + quantity + ", reserved " + reserved);
        }
        return new Event(id, name, date, place, totalCapacity,
                available, reserved - quantity, sold + quantity, complimentary);
    }

    /**
     * Releases the given quantity of reserved tickets back to available, returning a new
     * immutable {@link Event} with adjusted counters: {@code reserved − quantity},
     * {@code available + quantity}.
     *
     * @param quantity the number of tickets to release (must be positive)
     * @return a new {@link Event} with updated counters
     * @throws InvalidEventException if quantity is invalid or insufficient reserved
     */
    public Event release(int quantity) {
        if (quantity <= 0) {
            throw new InvalidEventException("Release quantity must be positive");
        }
        if (reserved < quantity) {
            throw new InvalidEventException(
                    "Not enough reserved tickets: requested " + quantity + ", reserved " + reserved);
        }
        return new Event(id, name, date, place, totalCapacity,
                available + quantity, reserved - quantity, sold, complimentary);
    }

    /**
     * Grants the given quantity of complimentary tickets, returning a new immutable
     * {@link Event} with adjusted counters: {@code available − quantity},
     * {@code complimentary + quantity}.
     *
     * @param quantity the number of complimentary tickets (must be positive)
     * @return a new {@link Event} with updated counters
     * @throws InvalidEventException if quantity is invalid or insufficient availability
     */
    public Event grantComplimentary(int quantity) {
        if (quantity <= 0) {
            throw new InvalidEventException("Complimentary quantity must be positive");
        }
        if (available < quantity) {
            throw new InvalidEventException(
                    "Not enough available tickets: requested " + quantity + ", available " + available);
        }
        return new Event(id, name, date, place, totalCapacity,
                available - quantity, reserved, sold, complimentary + quantity);
    }

    public EventId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public LocalDateTime date() {
        return date;
    }

    public String place() {
        return place;
    }

    public int totalCapacity() {
        return totalCapacity;
    }

    public int available() {
        return available;
    }

    public int reserved() {
        return reserved;
    }

    public int sold() {
        return sold;
    }

    public int complimentary() {
        return complimentary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Event{id=" + id + ", name='" + name + "', date=" + date +
                ", place='" + place + "', totalCapacity=" + totalCapacity +
                ", available=" + available + ", reserved=" + reserved +
                ", sold=" + sold + ", complimentary=" + complimentary + "}";
    }
}
