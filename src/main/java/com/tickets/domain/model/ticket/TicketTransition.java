package com.tickets.domain.model.ticket;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Audit record representing a single ticket status transition.
 *
 * <p>Every time a ticket changes status (AVAILABLE→RESERVED, RESERVED→SOLD, etc.),
 * a {@code TicketTransition} is persisted to the {@code ticket_transitions} table
 * for auditing purposes.</p>
 */
public final class TicketTransition {

    private final TicketTransitionId id;
    private final TicketId ticketId;
    private final TicketStatus fromStatus;
    private final TicketStatus toStatus;
    private final String orderId;
    private final LocalDateTime timestamp;

    private TicketTransition(TicketTransitionId id, TicketId ticketId,
                             TicketStatus fromStatus, TicketStatus toStatus,
                             String orderId, LocalDateTime timestamp) {
        this.id = id;
        this.ticketId = ticketId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.orderId = orderId;
        this.timestamp = timestamp;
    }

    /**
     * Creates a new transition record with a generated ID and current timestamp.
     */
    public static TicketTransition create(TicketId ticketId, TicketStatus fromStatus,
                                          TicketStatus toStatus, String orderId) {
        Objects.requireNonNull(ticketId, "Ticket ID must not be null");
        Objects.requireNonNull(fromStatus, "From status must not be null");
        Objects.requireNonNull(toStatus, "To status must not be null");
        return new TicketTransition(TicketTransitionId.generate(), ticketId,
                fromStatus, toStatus, orderId, LocalDateTime.now());
    }

    /**
     * Reconstitutes a transition from persistence (no validation).
     */
    public static TicketTransition reconstitute(TicketTransitionId id, TicketId ticketId,
                                                TicketStatus fromStatus, TicketStatus toStatus,
                                                String orderId, LocalDateTime timestamp) {
        return new TicketTransition(id, ticketId, fromStatus, toStatus, orderId, timestamp);
    }

    public TicketTransitionId id() { return id; }
    public TicketId ticketId() { return ticketId; }
    public TicketStatus fromStatus() { return fromStatus; }
    public TicketStatus toStatus() { return toStatus; }
    public String orderId() { return orderId; }
    public LocalDateTime timestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicketTransition that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "TicketTransition{id=" + id + ", ticketId=" + ticketId +
                ", from=" + fromStatus + ", to=" + toStatus +
                ", orderId=" + orderId + ", timestamp=" + timestamp + "}";
    }
}
