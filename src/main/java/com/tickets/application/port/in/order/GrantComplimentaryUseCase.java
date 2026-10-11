package com.tickets.application.port.in.order;

import com.tickets.application.usecase.order.GrantComplimentaryCommand;
import com.tickets.domain.model.order.Order;
import reactor.core.publisher.Mono;

/**
 * Use case for granting complimentary tickets to a customer.
 */
public interface GrantComplimentaryUseCase {

    Mono<Order> grant(GrantComplimentaryCommand command);
}
