package com.tickets.infrastructure.adapter.in.web;

import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.exception.InvalidEventException;
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
}
