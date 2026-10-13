package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.domain.model.ticket.TicketTransition;
import com.tickets.domain.model.ticket.TicketTransitionId;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketTransitionEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTransitionPersistenceMapperTest {

    @Test
    void toEntity_mapsAllFields() {
        TicketTransitionId id = TicketTransitionId.generate();
        TicketId ticketId = TicketId.generate();
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 11, 15, 30);

        TicketTransition transition = TicketTransition.reconstitute(
                id, ticketId, TicketStatus.AVAILABLE, TicketStatus.RESERVED, "order-123", timestamp);

        TicketTransitionEntity entity = TicketTransitionPersistenceMapper.toEntity(transition);

        assertThat(entity.getTransitionId()).isEqualTo(id.value().toString());
        assertThat(entity.getTicketId()).isEqualTo(ticketId.value().toString());
        assertThat(entity.getFromStatus()).isEqualTo("AVAILABLE");
        assertThat(entity.getToStatus()).isEqualTo("RESERVED");
        assertThat(entity.getOrderId()).isEqualTo("order-123");
        assertThat(entity.getTimestamp()).isEqualTo(timestamp.toString());
    }

    @Test
    void toDomain_mapsAllFields() {
        TicketTransitionId id = TicketTransitionId.generate();
        TicketId ticketId = TicketId.generate();
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 11, 15, 30);

        TicketTransitionEntity entity = new TicketTransitionEntity();
        entity.setTransitionId(id.value().toString());
        entity.setTicketId(ticketId.value().toString());
        entity.setFromStatus("RESERVED");
        entity.setToStatus("SOLD");
        entity.setOrderId("order-456");
        entity.setTimestamp(timestamp.toString());

        TicketTransition transition = TicketTransitionPersistenceMapper.toDomain(entity);

        assertThat(transition.id()).isEqualTo(id);
        assertThat(transition.ticketId()).isEqualTo(ticketId);
        assertThat(transition.fromStatus()).isEqualTo(TicketStatus.RESERVED);
        assertThat(transition.toStatus()).isEqualTo(TicketStatus.SOLD);
        assertThat(transition.orderId()).isEqualTo("order-456");
        assertThat(transition.timestamp()).isEqualTo(timestamp);
    }

    @Test
    void roundTrip_preservesAllFields() {
        TicketId ticketId = TicketId.generate();
        TicketTransition original = TicketTransition.create(
                ticketId, TicketStatus.AVAILABLE, TicketStatus.COMPLIMENTARY, "order-789");

        TicketTransitionEntity entity = TicketTransitionPersistenceMapper.toEntity(original);
        TicketTransition roundTripped = TicketTransitionPersistenceMapper.toDomain(entity);

        assertThat(roundTripped.id()).isEqualTo(original.id());
        assertThat(roundTripped.ticketId()).isEqualTo(original.ticketId());
        assertThat(roundTripped.fromStatus()).isEqualTo(original.fromStatus());
        assertThat(roundTripped.toStatus()).isEqualTo(original.toStatus());
        assertThat(roundTripped.orderId()).isEqualTo(original.orderId());
        assertThat(roundTripped.timestamp()).isEqualTo(original.timestamp());
    }
}
