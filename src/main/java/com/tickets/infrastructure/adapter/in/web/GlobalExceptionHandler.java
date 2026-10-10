package com.tickets.infrastructure.adapter.in.web;

import com.tickets.domain.exception.DomainException;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.exception.InvalidTicketException;
import com.tickets.domain.exception.TicketNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Centralized exception handler for all REST endpoints.
 *
 * <p>Uses pattern matching over the sealed {@link DomainException} hierarchy
 * to map domain exceptions to appropriate HTTP status codes.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(DomainException ex) {
        HttpStatus status = switch (ex) {
            case EventNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case InvalidEventException ignored -> HttpStatus.BAD_REQUEST;
            case TicketNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case InvalidTicketException ignored -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status)
                .body(Map.of("error", ex.message()));
    }
}
