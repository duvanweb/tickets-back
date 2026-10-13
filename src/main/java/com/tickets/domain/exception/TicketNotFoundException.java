package com.tickets.domain.exception;

/**
 * Thrown when a ticket is not found by its identifier.
 */
public final class TicketNotFoundException extends DomainException {

    public TicketNotFoundException(String message) {
        super(message);
    }
}
