package com.tickets.infrastructure.adapter.in.web;

import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.exception.InvalidOrderException;
import com.tickets.domain.exception.InvalidTicketException;
import com.tickets.domain.exception.OrderNotFoundException;
import com.tickets.domain.exception.TicketNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEventNotFoundException_returns404() {
        EventNotFoundException ex = new EventNotFoundException("Event not found with id: 123");

        ResponseEntity<Map<String, String>> response = handler.handleDomainException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Event not found with id: 123");
    }

    @Test
    void handleInvalidEventException_returns400() {
        InvalidEventException ex = new InvalidEventException("Event name must not be blank");

        ResponseEntity<Map<String, String>> response = handler.handleDomainException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Event name must not be blank");
    }

    @Test
    void handleTicketNotFoundException_returns404() {
        TicketNotFoundException ex = new TicketNotFoundException("Ticket not found with id: abc");

        ResponseEntity<Map<String, String>> response = handler.handleDomainException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Ticket not found with id: abc");
    }

    @Test
    void handleInvalidTicketException_returns400() {
        InvalidTicketException ex = new InvalidTicketException("Cannot transition from SOLD to AVAILABLE");

        ResponseEntity<Map<String, String>> response = handler.handleDomainException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Cannot transition from SOLD to AVAILABLE");
    }

    @Test
    void handleOrderNotFoundException_returns404() {
        OrderNotFoundException ex = new OrderNotFoundException("Order not found with id: 123");

        ResponseEntity<Map<String, String>> response = handler.handleDomainException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Order not found with id: 123");
    }

    @Test
    void handleInvalidOrderException_returns400() {
        InvalidOrderException ex = new InvalidOrderException("Quantity must be positive");

        ResponseEntity<Map<String, String>> response = handler.handleDomainException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Quantity must be positive");
    }
}
