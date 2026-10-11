package com.tickets.infrastructure.adapter.in.web.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for granting complimentary tickets.
 *
 * @param customerId the customer receiving the complimentary tickets
 * @param quantity   the number of complimentary tickets
 */
public record GrantComplimentaryRequest(
        @NotBlank String customerId,
        @Positive int quantity) {
}
