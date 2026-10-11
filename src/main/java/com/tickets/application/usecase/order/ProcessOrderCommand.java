package com.tickets.application.usecase.order;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.OrderId;

/**
 * Command for processing an order from the queue.
 *
 * @param orderId    the order ID to process
 * @param eventId    the event ID to reserve tickets for
 * @param customerId the customer placing the order
 * @param quantity   the number of tickets requested
 */
public record ProcessOrderCommand(OrderId orderId, EventId eventId, String customerId, int quantity) {
}
