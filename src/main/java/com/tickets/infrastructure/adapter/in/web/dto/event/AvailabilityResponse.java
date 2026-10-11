package com.tickets.infrastructure.adapter.in.web.dto.event;

/**
 * Response DTO for real-time ticket availability.
 *
 * @param eventId      the event ID
 * @param totalCapacity the total ticket capacity
 * @param available    the number of available tickets
 * @param reserved     the number of reserved tickets
 * @param sold         the number of sold tickets
 * @param complimentary the number of complimentary tickets
 */
public record AvailabilityResponse(
        String eventId,
        int totalCapacity,
        int available,
        int reserved,
        int sold,
        int complimentary) {
}
