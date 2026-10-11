package com.tickets.domain.model.ticket;

import com.tickets.domain.exception.InvalidTicketException;
import com.tickets.domain.model.event.EventId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root representing a single ticket (entry) for an event.
 *
 * <p>Each ticket has a state ({@link TicketStatus}), belongs to an event,
 * and optionally tracks the order/customer it is associated with and the
 * reservation TTL. Instances are created via {@link #create} (new ticket)
 * or {@link #reconstitute} (from persistence). State transitions are
 * validated through {@link #canTransitionTo} / {@link #transitionTo}.</p>
 */
public final class Ticket {

    private final TicketId id;
    private final EventId eventId;
    private final TicketStatus status;
    private final String seatNumber;
    private final String orderId;
    private final String customerId;
    private final LocalDateTime reservedUntil;
    private final long version;

    private Ticket(TicketId id, EventId eventId, TicketStatus status, String seatNumber,
                   String orderId, String customerId, LocalDateTime reservedUntil, long version) {
        this.id = id;
        this.eventId = eventId;
        this.status = status;
        this.seatNumber = seatNumber;
        this.orderId = orderId;
        this.customerId = customerId;
        this.reservedUntil = reservedUntil;
        this.version = version;
    }

    /**
     * Factory method that creates a new {@link Ticket} in {@link TicketStatus#AVAILABLE}
     * with a generated ID and version 0.
     *
     * @param eventId    the event this ticket belongs to
     * @param seatNumber the seat number (must not be blank)
     * @return a new {@link Ticket} in AVAILABLE status
     * @throws InvalidTicketException if validation fails
     */
    public static Ticket create(EventId eventId, String seatNumber) {
        validate(eventId, seatNumber);
        return new Ticket(TicketId.generate(), eventId, TicketStatus.AVAILABLE, seatNumber,
                null, null, null, 0);
    }

    /**
     * Rebuilds a {@link Ticket} from persistence without validation or ID generation.
     *
     * @param id           the existing ticket ID
     * @param eventId      the event ID
     * @param status       the ticket status
     * @param seatNumber   the seat number
     * @param orderId      the associated order ID (nullable)
     * @param customerId   the associated customer ID (nullable)
     * @param reservedUntil the reservation TTL (nullable)
     * @param version      the optimistic locking version
     * @return a reconstituted {@link Ticket}
     */
    public static Ticket reconstitute(TicketId id, EventId eventId, TicketStatus status,
                                       String seatNumber, String orderId, String customerId,
                                       LocalDateTime reservedUntil, long version) {
        return new Ticket(id, eventId, status, seatNumber, orderId, customerId, reservedUntil, version);
    }

    private static void validate(EventId eventId, String seatNumber) {
        if (eventId == null) {
            throw new InvalidTicketException("Event ID must not be null");
        }
        if (seatNumber == null || seatNumber.isBlank()) {
            throw new InvalidTicketException("Seat number must not be blank");
        }
    }

    /**
     * Checks whether this ticket can transition to the target status.
     *
     * @param target the desired target status
     * @return {@code true} if the transition is allowed
     */
    public boolean canTransitionTo(TicketStatus target) {
        return status.canTransitionTo(target);
    }

    /**
     * Transitions this ticket to the target status, returning a new immutable instance
     * with an incremented version.
     *
     * @param newStatus the target status
     * @return a new {@link Ticket} with the updated status and version + 1
     * @throws InvalidTicketException if the transition is not valid
     */
    public Ticket transitionTo(TicketStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new InvalidTicketException(
                    "Cannot transition from " + status + " to " + newStatus);
        }
        return new Ticket(id, eventId, newStatus, seatNumber, orderId, customerId,
                reservedUntil, version + 1);
    }

    /**
     * Reserves this ticket by transitioning to {@link TicketStatus#RESERVED} and setting
     * the associated order ID, customer ID, and reservation deadline.
     *
     * @param orderId      the order ID claiming this ticket
     * @param customerId   the customer ID claiming this ticket
     * @param reservedUntil the reservation TTL deadline
     * @return a new {@link Ticket} in RESERVED status with the given associations
     * @throws InvalidTicketException if the transition is not valid
     */
    public Ticket reserve(String orderId, String customerId, LocalDateTime reservedUntil) {
        if (!canTransitionTo(TicketStatus.RESERVED)) {
            throw new InvalidTicketException(
                    "Cannot transition from " + status + " to RESERVED");
        }
        return new Ticket(id, eventId, TicketStatus.RESERVED, seatNumber, orderId,
                customerId, reservedUntil, version + 1);
    }

    public TicketId id() {
        return id;
    }

    public EventId eventId() {
        return eventId;
    }

    public TicketStatus status() {
        return status;
    }

    public String seatNumber() {
        return seatNumber;
    }

    public String orderId() {
        return orderId;
    }

    public String customerId() {
        return customerId;
    }

    public LocalDateTime reservedUntil() {
        return reservedUntil;
    }

    public long version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return Objects.equals(id, ticket.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ticket{id=" + id + ", eventId=" + eventId + ", status=" + status +
                ", seatNumber='" + seatNumber + "', orderId=" + orderId +
                ", customerId=" + customerId + ", reservedUntil=" + reservedUntil +
                ", version=" + version + "}";
    }
}
