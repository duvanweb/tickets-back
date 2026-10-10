package com.tickets.domain.model.event;

import com.tickets.domain.exception.InvalidEventException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root representing an event.
 *
 * <p>Contains basic information: name, date, place, and total capacity.
 * Instances are created via the {@link #create} factory method, which
 * enforces all invariants. The {@link #reconstitute} method rebuilds
 * the aggregate from persistence without re-validating.</p>
 */
public final class Event {

    private final EventId id;
    private final String name;
    private final LocalDateTime date;
    private final String place;
    private final int totalCapacity;

    private Event(EventId id, String name, LocalDateTime date, String place, int totalCapacity) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.place = place;
        this.totalCapacity = totalCapacity;
    }

    /**
     * Factory method that creates a new {@link Event} with a generated ID.
     *
     * <p>Validates that name and place are not blank, date is not null,
     * and totalCapacity is positive.</p>
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
        return new Event(EventId.generate(), name, date, place, totalCapacity);
    }

    /**
     * Rebuilds an {@link Event} from persistence without validation or ID generation.
     *
     * @param id           the existing event ID
     * @param name         the event name
     * @param date         the event date
     * @param place        the event location
     * @param totalCapacity the total ticket capacity
     * @return a reconstituted {@link Event}
     */
    public static Event reconstitute(EventId id, String name, LocalDateTime date, String place, int totalCapacity) {
        return new Event(id, name, date, place, totalCapacity);
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
                ", place='" + place + "', totalCapacity=" + totalCapacity + "}";
    }
}
