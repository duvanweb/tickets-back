package com.tickets.infrastructure.adapter.in.web.dto.event;

import java.time.LocalDateTime;

/**
 * Response DTO representing an event.
 *
 * @param id           the event ID
 * @param name         the event name
 * @param date         the event date and time
 * @param place        the event location
 * @param totalCapacity the total ticket capacity
 */
public record EventResponse(
        String id,
        String name,
        LocalDateTime date,
        String place,
        int totalCapacity) {
}
