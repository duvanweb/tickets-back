package com.tickets.domain.model.ticket;

/**
 * State machine for a {@link Ticket}.
 *
 * <p>Transitions:
 * <pre>
 *   AVAILABLE → RESERVED, COMPLIMENTARY
 *   RESERVED  → PENDING_CONFIRMATION, AVAILABLE
 *   PENDING_CONFIRMATION → SOLD, AVAILABLE
 *   SOLD      → (terminal)
 *   COMPLIMENTARY → (terminal)
 * </pre>
 */
public enum TicketStatus {

    AVAILABLE,
    RESERVED,
    PENDING_CONFIRMATION,
    SOLD,
    COMPLIMENTARY;

    /**
     * Checks whether a transition from this status to the target is valid.
     *
     * @param target the desired target status
     * @return {@code true} if the transition is allowed, {@code false} otherwise
     */
    public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case AVAILABLE            -> target == RESERVED || target == COMPLIMENTARY;
            case RESERVED             -> target == PENDING_CONFIRMATION || target == AVAILABLE;
            case PENDING_CONFIRMATION -> target == SOLD || target == AVAILABLE;
            case SOLD, COMPLIMENTARY  -> false;
        };
    }
}
