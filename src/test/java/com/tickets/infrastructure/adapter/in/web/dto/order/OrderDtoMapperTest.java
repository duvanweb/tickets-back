package com.tickets.infrastructure.adapter.in.web.dto.order;

import com.tickets.application.port.in.order.CreateOrderUseCase.CreateOrderCommand;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderStatus;
import com.tickets.domain.model.ticket.TicketId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDtoMapperTest {

    @Test
    void toCommand_mapsAllFields() {
        EventId eventId = EventId.generate();
        CreateOrderRequest request = new CreateOrderRequest(
                eventId.value().toString(), "customer-123", 3);

        CreateOrderCommand command = OrderDtoMapper.toCommand(request);

        assertThat(command.eventId()).isEqualTo(eventId);
        assertThat(command.customerId()).isEqualTo("customer-123");
        assertThat(command.quantity()).isEqualTo(3);
    }

    @Test
    void toResponse_mapsAllFields() {
        EventId eventId = EventId.generate();
        TicketId ticketId1 = TicketId.generate();
        TicketId ticketId2 = TicketId.generate();
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 10, 14, 30);
        LocalDateTime reservedUntil = LocalDateTime.of(2026, 10, 10, 14, 35);

        Order order = Order.reconstitute(
                com.tickets.domain.model.order.OrderId.generate(),
                eventId, "customer-123", 2, OrderStatus.RESERVED,
                createdAt, reservedUntil, List.of(ticketId1, ticketId2));

        OrderResponse response = OrderDtoMapper.toResponse(order);

        assertThat(response.eventId()).isEqualTo(eventId.value().toString());
        assertThat(response.customerId()).isEqualTo("customer-123");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.reservedUntil()).isEqualTo(reservedUntil);
        assertThat(response.ticketIds()).hasSize(2);
        assertThat(response.ticketIds().get(0)).isEqualTo(ticketId1.value().toString());
    }

    @Test
    void toResponse_withNullReservedUntil_mapsToNull() {
        Order order = Order.create(EventId.generate(), "customer-123", 1);

        OrderResponse response = OrderDtoMapper.toResponse(order);

        assertThat(response.reservedUntil()).isNull();
        assertThat(response.ticketIds()).isEmpty();
        assertThat(response.status()).isEqualTo("PENDING");
    }
}
