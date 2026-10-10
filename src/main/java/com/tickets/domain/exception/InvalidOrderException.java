package com.tickets.domain.exception;

/**
 * Thrown when an order is in an invalid state or receives invalid input.
 */
public final class InvalidOrderException extends DomainException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
