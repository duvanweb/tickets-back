package com.tickets.domain.exception;

/**
 * Base sealed class for all domain-level exceptions.
 *
 * <p>Uses a sealed hierarchy so that exception handling can rely on
 * pattern matching over a closed set of permitted subclasses.</p>
 */
public abstract sealed class DomainException extends RuntimeException
        permits InvalidEventException, EventNotFoundException,
                InvalidTicketException, TicketNotFoundException,
                InvalidOrderException, OrderNotFoundException {

    protected DomainException(String message) {
        super(message);
    }

    public String message() {
        return getMessage();
    }
}
