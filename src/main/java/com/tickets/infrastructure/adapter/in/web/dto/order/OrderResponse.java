package com.tickets.infrastructure.adapter.in.web.dto.order;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing an order.
 *
 * @param id           the order ID
 * @param eventId      the event ID
 * @param customerId   the customer ID
 * @param quantity     the ticket quantity
 * @param status       the order status
 * @param createdAt    the creation timestamp
 * @param reservedUntil the reservation deadline (nullable)
 * @param ticketIds    the associated ticket IDs
 */
public record OrderResponse(
        String id,
        String eventId,
        String customerId,
        int quantity,
        String status,
        LocalDateTime createdAt,
        LocalDateTime reservedUntil,
        List<String> ticketIds) {
}
