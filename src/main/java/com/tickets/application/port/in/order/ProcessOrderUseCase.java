package com.tickets.application.port.in.order;

import com.tickets.application.usecase.order.ProcessOrderCommand;
import com.tickets.domain.model.order.Order;
import reactor.core.publisher.Mono;

/**
 * Use case for processing an order message from the queue.
 *
 * <p>Attempts to reserve tickets for the order. On success the order
 * transitions to {@code RESERVED}; on insufficient availability it
 * transitions to {@code REJECTED}.</p>
 */
public interface ProcessOrderUseCase {

    Mono<Order> process(ProcessOrderCommand command);
}
