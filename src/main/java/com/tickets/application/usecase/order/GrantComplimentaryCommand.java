package com.tickets.application.usecase.order;

import com.tickets.domain.model.event.EventId;

/**
 * Command for granting complimentary tickets.
 *
 * @param eventId   the event to grant complimentary tickets for
 * @param customerId the customer receiving the complimentary tickets
 * @param quantity   the number of complimentary tickets
 */
public record GrantComplimentaryCommand(EventId eventId, String customerId, int quantity) {
}
