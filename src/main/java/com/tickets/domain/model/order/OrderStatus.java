package com.tickets.domain.model.order;

/**
 * Lifecycle status of an {@link Order}.
 *
 * <pre>
 * PENDING ─(consumer reserves N)─► RESERVED ─(confirm)─► PENDING_CONFIRMATION ─(payment OK)─► SOLD
 *    │                                 │
 *    │ no availability                 │ TTL expires → scheduler
 *    ▼                                 ▼
 * REJECTED                          EXPIRED
 *
 * (admin flow) ──────────────────────────────────────────────────────► COMPLIMENTARY
 * </pre>
 */
public enum OrderStatus {

    PENDING,
    RESERVED,
    PENDING_CONFIRMATION,
    SOLD,
    REJECTED,
    EXPIRED,
    COMPLIMENTARY;

    /**
     * Checks whether this status can transition to the target status.
     *
     * @param target the desired target status
     * @return {@code true} if the transition is allowed
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING              -> target == RESERVED || target == REJECTED;
            case RESERVED             -> target == PENDING_CONFIRMATION || target == EXPIRED || target == SOLD;
            case PENDING_CONFIRMATION -> target == SOLD;
            case SOLD, REJECTED, EXPIRED, COMPLIMENTARY -> false;
        };
    }
}
