package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.domain.model.ticket.TicketTransition;
import com.tickets.domain.model.ticket.TicketTransitionId;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketTransitionEntity;

import java.time.LocalDateTime;

/**
 * Manual mapper between {@link TicketTransition} domain and {@link TicketTransitionEntity}.
 */
public final class TicketTransitionPersistenceMapper {

    private TicketTransitionPersistenceMapper() {
    }

    public static TicketTransitionEntity toEntity(TicketTransition transition) {
        TicketTransitionEntity entity = new TicketTransitionEntity();
        entity.setTransitionId(transition.id().value().toString());
        entity.setTicketId(transition.ticketId().value().toString());
        entity.setFromStatus(transition.fromStatus().name());
        entity.setToStatus(transition.toStatus().name());
        entity.setOrderId(transition.orderId());
        entity.setTimestamp(transition.timestamp().toString());
        return entity;
    }

    public static TicketTransition toDomain(TicketTransitionEntity entity) {
        return TicketTransition.reconstitute(
                TicketTransitionId.of(entity.getTransitionId()),
                TicketId.of(entity.getTicketId()),
                TicketStatus.valueOf(entity.getFromStatus()),
                TicketStatus.valueOf(entity.getToStatus()),
                entity.getOrderId(),
                LocalDateTime.parse(entity.getTimestamp()));
    }
}
