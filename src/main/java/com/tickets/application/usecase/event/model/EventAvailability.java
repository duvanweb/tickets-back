package com.tickets.application.usecase.event.model;

import com.tickets.domain.model.event.EventId;

/**
 * Read model representing real-time ticket availability for an event.
 *
 * @param eventId      the event ID
 * @param totalCapacity the total ticket capacity
 * @param available    the number of available tickets
 * @param reserved     the number of reserved tickets
 * @param sold         the number of sold tickets
 * @param complimentary the number of complimentary tickets
 */
public record EventAvailability(
        EventId eventId,
        int totalCapacity,
        int available,
        int reserved,
        int sold,
        int complimentary) {
}
