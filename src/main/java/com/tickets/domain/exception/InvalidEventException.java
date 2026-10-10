package com.tickets.domain.exception;

/**
 * Thrown when an event is in an invalid state or receives invalid input.
 */
public final class InvalidEventException extends DomainException {

    public InvalidEventException(String message) {
        super(message);
    }
}
