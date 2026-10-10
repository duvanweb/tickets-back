package com.tickets.domain.exception;

/**
 * Thrown when an event is not found by its identifier.
 */
public final class EventNotFoundException extends DomainException {

    public EventNotFoundException(String message) {
        super(message);
    }
}
