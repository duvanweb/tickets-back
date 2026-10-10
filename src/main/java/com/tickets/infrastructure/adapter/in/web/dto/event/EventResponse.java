package com.tickets.infrastructure.adapter.in.web.dto.event;

import java.time.LocalDate;

/**
 * Response DTO representing an event.
 *
 * @param id           the event ID
 * @param name         the event name
 * @param date         the event date
 * @param place        the event location
 * @param totalCapacity the total ticket capacity
 */
public record EventResponse(
        String id,
        String name,
        LocalDate date,
        String place,
        int totalCapacity) {
}
