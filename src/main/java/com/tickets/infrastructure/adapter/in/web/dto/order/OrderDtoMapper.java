package com.tickets.infrastructure.adapter.in.web.dto.order;

import com.tickets.application.usecase.order.CreateOrderCommand;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;

import java.util.List;

/**
 * Manual mapper between web DTOs and application/domain objects for orders.
 */
public final class OrderDtoMapper {

    private OrderDtoMapper() {
    }

    /**
     * Converts a {@link CreateOrderRequest} to a {@link CreateOrderCommand}.
     *
     * @param request the web request
     * @return the application command
     */
    public static CreateOrderCommand toCommand(CreateOrderRequest request) {
        return new CreateOrderCommand(
                EventId.of(request.eventId()),
                request.customerId(),
                request.quantity());
    }

    /**
     * Converts a domain {@link Order} to an {@link OrderResponse}.
     *
     * @param order the domain order
     * @return the web response
     */
    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.id().value().toString(),
                order.eventId().value().toString(),
                order.customerId(),
                order.quantity(),
                order.status().name(),
                order.createdAt(),
                order.reservedUntil(),
                order.ticketIds().stream()
                        .map(ticketId -> ticketId.value().toString())
                        .toList());
    }
}
