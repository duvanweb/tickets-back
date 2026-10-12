package com.tickets.infrastructure.adapter.in.web;

import com.tickets.domain.exception.DomainException;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.exception.InvalidOrderException;
import com.tickets.domain.exception.InvalidTicketException;
import com.tickets.domain.exception.OrderNotFoundException;
import com.tickets.domain.exception.TicketNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.List;
import java.util.Map;

/**
 * Centralized exception handler for all REST endpoints.
 *
 * <p>Uses pattern matching over the sealed {@link DomainException} hierarchy
 * to map domain exceptions to appropriate HTTP status codes. Also handles
 * {@link IllegalArgumentException} (e.g. invalid UUID) and validation
 * errors ({@link WebExchangeBindException}).</p>
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
            case OrderNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case InvalidOrderException ignored -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status)
                .body(Map.of("error", ex.message()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(WebExchangeBindException ex) {
        List<Map<String, String>> details = ex.getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value"))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Validation failed", "details", details));
    }
}
