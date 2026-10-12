package com.tickets.application.port.out.repositories;

/**
 * Result of an atomic ticket reservation attempt via DynamoDB TransactWriteItems.
 *
 * <p>Sealed interface consistent with the project's sealed type patterns.
 * Used by {@link TicketReservationRepository#reserveTicketsAtomically} to
 * communicate the transaction outcome to the application layer.</p>
 */
public sealed interface ReservationResult {

    /**
     * The transaction committed successfully — all tickets are reserved
     * and event counters are updated.
     */
    record Success() implements ReservationResult {}

    /**
     * The Event condition {@code available >= :qty} failed — not enough
     * tickets available at write time. Not retryable; the order should be rejected.
     */
    record EventConditionFailed() implements ReservationResult {}

    /**
     * One or more Ticket conditions {@code status = :available} failed —
     * tickets were claimed by another concurrent transaction. Retryable
     * with a fresh set of available tickets.
     */
    record TicketConditionFailed() implements ReservationResult {}
}
