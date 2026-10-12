package com.tickets.infrastructure.adapter.in.web.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating a new order.
 *
 * @param eventId   the event ID to buy tickets for
 * @param customerId the customer placing the order
 * @param quantity   the number of tickets requested (1-99)
 */
public record CreateOrderRequest(
        @NotBlank String eventId,
        @NotBlank String customerId,
        @Positive @Max(99) int quantity) {
}
