package com.tickets.application.usecase.order;

import com.tickets.domain.model.event.EventId;

/**
 * Command for creating a new order.
 *
 * @param eventId   the event to buy tickets for
 * @param customerId the customer placing the order
 * @param quantity   the number of tickets requested
 */
public record CreateOrderCommand(EventId eventId, String customerId, int quantity) {
}
