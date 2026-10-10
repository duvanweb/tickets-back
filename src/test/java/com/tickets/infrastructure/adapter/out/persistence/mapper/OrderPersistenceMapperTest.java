package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.order.OrderStatus;
import com.tickets.domain.model.ticket.TicketId;
import com.tickets.infrastructure.adapter.out.persistence.entity.OrderEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPersistenceMapperTest {

    private static final OrderId ORDER_ID = OrderId.generate();
    private static final EventId EVENT_ID = EventId.generate();
    private static final String CUSTOMER_ID = "customer-123";
    private static final int QUANTITY = 3;
    private static final OrderStatus STATUS = OrderStatus.RESERVED;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 10, 14, 30);
    private static final LocalDateTime RESERVED_UNTIL = LocalDateTime.of(2026, 10, 10, 14, 35);
    private static final List<TicketId> TICKET_IDS = List.of(TicketId.generate(), TicketId.generate());

    @Test
    void toEntity_mapsAllFields() {
        Order order = Order.reconstitute(ORDER_ID, EVENT_ID, CUSTOMER_ID, QUANTITY,
                STATUS, CREATED_AT, RESERVED_UNTIL, TICKET_IDS);

        OrderEntity entity = OrderPersistenceMapper.toEntity(order);

        assertThat(entity.getOrderId()).isEqualTo(ORDER_ID.value().toString());
        assertThat(entity.getEventId()).isEqualTo(EVENT_ID.value().toString());
        assertThat(entity.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(entity.getQuantity()).isEqualTo(QUANTITY);
        assertThat(entity.getStatus()).isEqualTo(STATUS.name());
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT.toString());
        assertThat(entity.getReservedUntil()).isEqualTo(RESERVED_UNTIL.toString());
        assertThat(entity.getTicketIds()).hasSize(2);
        assertThat(entity.getTicketIds().get(0)).isEqualTo(TICKET_IDS.get(0).value().toString());
    }

    @Test
    void toEntity_withNullReservedUntil_mapsToNull() {
        Order order = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        OrderEntity entity = OrderPersistenceMapper.toEntity(order);

        assertThat(entity.getReservedUntil()).isNull();
        assertThat(entity.getTicketIds()).isEmpty();
    }

    @Test
    void toDomain_mapsAllFields() {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(ORDER_ID.value().toString());
        entity.setEventId(EVENT_ID.value().toString());
        entity.setCustomerId(CUSTOMER_ID);
        entity.setQuantity(QUANTITY);
        entity.setStatus(STATUS.name());
        entity.setCreatedAt(CREATED_AT.toString());
        entity.setReservedUntil(RESERVED_UNTIL.toString());
        entity.setTicketIds(TICKET_IDS.stream().map(t -> t.value().toString()).toList());

        Order order = OrderPersistenceMapper.toDomain(entity);

        assertThat(order.id()).isEqualTo(ORDER_ID);
        assertThat(order.eventId()).isEqualTo(EVENT_ID);
        assertThat(order.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.quantity()).isEqualTo(QUANTITY);
        assertThat(order.status()).isEqualTo(STATUS);
        assertThat(order.createdAt()).isEqualTo(CREATED_AT);
        assertThat(order.reservedUntil()).isEqualTo(RESERVED_UNTIL);
        assertThat(order.ticketIds()).hasSize(2);
        assertThat(order.ticketIds().get(0)).isEqualTo(TICKET_IDS.get(0));
    }

    @Test
    void toDomain_withNullReservedUntilAndNullTicketIds_mapsToNullAndEmptyList() {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(ORDER_ID.value().toString());
        entity.setEventId(EVENT_ID.value().toString());
        entity.setCustomerId(CUSTOMER_ID);
        entity.setQuantity(QUANTITY);
        entity.setStatus(OrderStatus.PENDING.name());
        entity.setCreatedAt(CREATED_AT.toString());

        Order order = OrderPersistenceMapper.toDomain(entity);

        assertThat(order.reservedUntil()).isNull();
        assertThat(order.ticketIds()).isEmpty();
    }

    @Test
    void toEntityAndBack_preservesAllFields() {
        Order original = Order.reconstitute(ORDER_ID, EVENT_ID, CUSTOMER_ID, QUANTITY,
                STATUS, CREATED_AT, RESERVED_UNTIL, TICKET_IDS);

        Order roundTripped = OrderPersistenceMapper.toDomain(OrderPersistenceMapper.toEntity(original));

        assertThat(roundTripped).isEqualTo(original);
        assertThat(roundTripped.eventId()).isEqualTo(EVENT_ID);
        assertThat(roundTripped.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(roundTripped.quantity()).isEqualTo(QUANTITY);
        assertThat(roundTripped.status()).isEqualTo(STATUS);
        assertThat(roundTripped.createdAt()).isEqualTo(CREATED_AT);
        assertThat(roundTripped.reservedUntil()).isEqualTo(RESERVED_UNTIL);
        assertThat(roundTripped.ticketIds()).isEqualTo(TICKET_IDS);
    }
}
