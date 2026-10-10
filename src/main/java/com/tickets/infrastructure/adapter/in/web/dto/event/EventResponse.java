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
 * @param available     the available ticket count
 * @param reserved      the reserved ticket count
 * @param sold          the sold ticket count
 * @param complimentary the complimentary ticket count
 */
public record EventResponse(
        String id,
        String name,
        LocalDateTime date,
        String place,
        int totalCapacity,
        int available,
        int reserved,
        int sold,
        int complimentary) {
}
