package com.tickets.application.port.in.order;

import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import reactor.core.publisher.Mono;

/**
 * Use case for confirming an order (payment confirmation).
 *
 * <p>Validates that the order is in {@code RESERVED} status and the reservation
 * has not expired, then transitions tickets to {@code SOLD}, updates event
 * counters, and transitions the order to {@code SOLD}.</p>
 */
public interface ConfirmOrderUseCase {

    Mono<Order> confirm(OrderId id);
}
