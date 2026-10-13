package com.tickets.application.port.in.order;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import reactor.core.publisher.Mono;

/**
 * Use case for granting complimentary tickets to a customer.
 */
public interface GrantComplimentaryUseCase {

    /**
     * Command for granting complimentary tickets.
     *
     * @param eventId   the event to grant complimentary tickets for
     * @param customerId the customer receiving the complimentary tickets
     * @param quantity   the number of complimentary tickets
     */
    record GrantComplimentaryCommand(EventId eventId, String customerId, int quantity) {}

    Mono<Order> grant(GrantComplimentaryCommand command);
}
