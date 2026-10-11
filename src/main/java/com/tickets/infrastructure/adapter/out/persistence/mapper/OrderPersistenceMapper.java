package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.order.OrderStatus;
import com.tickets.domain.model.ticket.TicketId;
import com.tickets.infrastructure.adapter.out.persistence.entity.OrderEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manual mapper between the domain {@link Order} and the persistence {@link OrderEntity}.
 */
public final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {
    }

    /**
     * Converts a domain {@link Order} to a persistence {@link OrderEntity}.
     *
     * @param order the domain order
     * @return the DynamoDB entity
     */
    public static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(order.id().value().toString());
        entity.setEventId(order.eventId().value().toString());
        entity.setCustomerId(order.customerId());
        entity.setQuantity(order.quantity());
        entity.setStatus(order.status().name());
        entity.setCreatedAt(order.createdAt().toString());
        entity.setReservedUntil(order.reservedUntil() != null ? order.reservedUntil().toString() : null);
        entity.setTicketIds(order.ticketIds().stream()
                .map(ticketId -> ticketId.value().toString())
                .toList());
        return entity;
    }

    /**
     * Converts a persistence {@link OrderEntity} to a domain {@link Order}.
     *
     * @param entity the DynamoDB entity
     * @return the domain order
     */
    public static Order toDomain(OrderEntity entity) {
        List<TicketId> ticketIds = entity.getTicketIds() != null
                ? entity.getTicketIds().stream().map(TicketId::of).toList()
                : List.of();
        return Order.reconstitute(
                OrderId.of(entity.getOrderId()),
                EventId.of(entity.getEventId()),
                entity.getCustomerId(),
                entity.getQuantity(),
                OrderStatus.valueOf(entity.getStatus()),
                LocalDateTime.parse(entity.getCreatedAt()),
                entity.getReservedUntil() != null ? LocalDateTime.parse(entity.getReservedUntil()) : null,
                ticketIds
        );
    }
}
