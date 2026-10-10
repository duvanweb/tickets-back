package com.tickets.application.usecase.event;

import java.time.LocalDate;

/**
 * Input command for creating an event.
 *
 * @param name         the event name
 * @param date         the event date
 * @param place        the event location
 * @param totalCapacity the total ticket capacity
 */
public record CreateEventCommand(String name, LocalDate date, String place, int totalCapacity) {
}
