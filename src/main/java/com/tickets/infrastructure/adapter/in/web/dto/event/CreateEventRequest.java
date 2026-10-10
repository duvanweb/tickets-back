package com.tickets.infrastructure.adapter.in.web.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Request DTO for creating an event.
 *
 * @param name         the event name (must not be blank)
 * @param date         the event date (must not be null)
 * @param place        the event location (must not be blank)
 * @param totalCapacity the total ticket capacity (must be positive)
 */
public record CreateEventRequest(
        @NotBlank String name,
        @NotNull LocalDate date,
        @NotBlank String place,
        @Positive int totalCapacity) {
}
