package com.tickets.application.usecase.event;

import java.time.LocalDateTime;

/**
 * Input command for creating an event.
 *
 * @param name         the event name
 * @param date         the event date and time
 * @param place        the event location
 * @param totalCapacity the total ticket capacity
 */
public record CreateEventCommand(String name, LocalDateTime date, String place, int totalCapacity) {
}
