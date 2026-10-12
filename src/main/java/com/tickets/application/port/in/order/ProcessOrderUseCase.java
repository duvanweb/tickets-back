package com.tickets.application.port.in.order;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import reactor.core.publisher.Mono;

/**
 * Use case for processing an order message from the queue.
 *
 * <p>Attempts to reserve tickets for the order. On success the order
 * transitions to {@code RESERVED}; on insufficient availability it
 * transitions to {@code REJECTED}.</p>
 */
public interface ProcessOrderUseCase {

    /**
     * Command for processing an order from the queue.
     *
     * @param orderId    the order ID to process
     * @param eventId    the event ID to reserve tickets for
     * @param customerId the customer placing the order
     * @param quantity   the number of tickets requested
     */
    record ProcessOrderCommand(OrderId orderId, EventId eventId, String customerId, int quantity) {}

    Mono<Order> process(ProcessOrderCommand command);
}
