package com.tickets.domain.exception;

/**
 * Thrown when a ticket is in an invalid state or receives invalid input.
 */
public final class InvalidTicketException extends DomainException {

    public InvalidTicketException(String message) {
        super(message);
    }
}
