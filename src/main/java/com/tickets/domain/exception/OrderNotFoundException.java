package com.tickets.domain.exception;

/**
 * Thrown when an order is not found by its identifier.
 */
public final class OrderNotFoundException extends DomainException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
