package com.tickets.domain.model.order;

import com.tickets.domain.exception.InvalidOrderException;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.TicketId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final EventId EVENT_ID = EventId.generate();
    private static final String CUSTOMER_ID = "customer-123";
    private static final int QUANTITY = 3;

    @Test
    void create_withValidInputs_returnsPendingOrderWithGeneratedId() {
        Order order = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        assertThat(order.id()).isNotNull();
        assertThat(order.eventId()).isEqualTo(EVENT_ID);
        assertThat(order.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.quantity()).isEqualTo(QUANTITY);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.createdAt()).isNotNull();
        assertThat(order.reservedUntil()).isNull();
        assertThat(order.ticketIds()).isEmpty();
    }

    @Test
    void create_withNullEventId_throwsInvalidOrderException() {
        assertThatThrownBy(() -> Order.create(null, CUSTOMER_ID, QUANTITY))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Event ID");
    }

    @Test
    void create_withBlankCustomerId_throwsInvalidOrderException() {
        assertThatThrownBy(() -> Order.create(EVENT_ID, "", QUANTITY))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Customer ID");
    }

    @Test
    void create_withNullCustomerId_throwsInvalidOrderException() {
        assertThatThrownBy(() -> Order.create(EVENT_ID, null, QUANTITY))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Customer ID");
    }

    @Test
    void create_withZeroQuantity_throwsInvalidOrderException() {
        assertThatThrownBy(() -> Order.create(EVENT_ID, CUSTOMER_ID, 0))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Quantity");
    }

    @Test
    void create_withNegativeQuantity_throwsInvalidOrderException() {
        assertThatThrownBy(() -> Order.create(EVENT_ID, CUSTOMER_ID, -1))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Quantity");
    }

    @Test
    void reconstitute_rebuildsOrderWithGivenValues() {
        OrderId id = OrderId.generate();
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 10, 14, 30);
        LocalDateTime reservedUntil = LocalDateTime.of(2026, 10, 10, 14, 35);
        List<TicketId> ticketIds = List.of(TicketId.generate(), TicketId.generate());

        Order order = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.RESERVED, createdAt, reservedUntil, ticketIds);

        assertThat(order.id()).isEqualTo(id);
        assertThat(order.eventId()).isEqualTo(EVENT_ID);
        assertThat(order.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.quantity()).isEqualTo(QUANTITY);
        assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(order.createdAt()).isEqualTo(createdAt);
        assertThat(order.reservedUntil()).isEqualTo(reservedUntil);
        assertThat(order.ticketIds()).hasSize(2);
    }

    @Test
    void reconstitute_withNullReservedUntilAndEmptyTicketIds_works() {
        OrderId id = OrderId.generate();
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 10, 14, 30);

        Order order = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.PENDING, createdAt, null, List.of());

        assertThat(order.reservedUntil()).isNull();
        assertThat(order.ticketIds()).isEmpty();
    }

    @Test
    void ordersWithSameId_areEqual() {
        OrderId id = OrderId.generate();
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 10, 14, 30);
        Order order1 = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.PENDING, createdAt, null, List.of());
        Order order2 = Order.reconstitute(id, EventId.generate(), "other", 1,
                OrderStatus.SOLD, createdAt.plusDays(1), null, List.of());

        assertThat(order1).isEqualTo(order2);
        assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
    }

    @Test
    void ordersWithDifferentId_areNotEqual() {
        Order order1 = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);
        Order order2 = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        assertThat(order1).isNotEqualTo(order2);
    }

    @Test
    void canTransitionTo_delegatesToStatus() {
        Order pending = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        assertThat(pending.canTransitionTo(OrderStatus.RESERVED)).isTrue();
        assertThat(pending.canTransitionTo(OrderStatus.SOLD)).isFalse();
    }

    @Test
    void reserve_withValidTransition_returnsReservedOrderWithTicketIds() {
        Order order = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);
        List<TicketId> ticketIds = List.of(TicketId.generate(), TicketId.generate(), TicketId.generate());
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(10);

        Order reserved = order.reserve(ticketIds, reservedUntil);

        assertThat(reserved.status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(reserved.ticketIds()).hasSize(3);
        assertThat(reserved.reservedUntil()).isEqualTo(reservedUntil);
        assertThat(reserved.id()).isEqualTo(order.id());
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void reserve_withNonPendingStatus_throwsInvalidOrderException() {
        OrderId id = OrderId.generate();
        Order reserved = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.RESERVED, LocalDateTime.now(), null, List.of());

        assertThatThrownBy(() -> reserved.reserve(List.of(TicketId.generate()), LocalDateTime.now()))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void reserve_withEmptyTicketIds_throwsInvalidOrderException() {
        Order order = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        assertThatThrownBy(() -> order.reserve(List.of(), LocalDateTime.now().plusMinutes(10)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("ticket IDs");
    }

    @Test
    void reserve_withNullReservedUntil_throwsInvalidOrderException() {
        Order order = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        assertThatThrownBy(() -> order.reserve(List.of(TicketId.generate()), null))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("deadline");
    }

    @Test
    void reject_withPendingOrder_returnsRejectedOrder() {
        Order order = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        Order rejected = order.reject();

        assertThat(rejected.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(rejected.id()).isEqualTo(order.id());
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void reject_withNonPendingStatus_throwsInvalidOrderException() {
        OrderId id = OrderId.generate();
        Order reserved = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.RESERVED, LocalDateTime.now(), null, List.of());

        assertThatThrownBy(reserved::reject)
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void isExpired_whenReservedUntilInPast_returnsTrue() {
        OrderId id = OrderId.generate();
        Order order = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.RESERVED, LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusMinutes(5), List.of());

        assertThat(order.isExpired()).isTrue();
    }

    @Test
    void isExpired_whenReservedUntilInFuture_returnsFalse() {
        OrderId id = OrderId.generate();
        Order order = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.RESERVED, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10), List.of());

        assertThat(order.isExpired()).isFalse();
    }

    @Test
    void isExpired_whenReservedUntilIsNull_returnsFalse() {
        Order order = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        assertThat(order.isExpired()).isFalse();
    }

    @Test
    void confirm_withValidReservedOrder_returnsSoldOrder() {
        OrderId id = OrderId.generate();
        List<TicketId> ticketIds = List.of(TicketId.generate(), TicketId.generate());
        Order reserved = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.RESERVED, LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10), ticketIds);

        Order sold = reserved.confirm();

        assertThat(sold.status()).isEqualTo(OrderStatus.SOLD);
        assertThat(sold.id()).isEqualTo(id);
        assertThat(sold.ticketIds()).isEqualTo(ticketIds);
    }

    @Test
    void confirm_withNonReservedStatus_throwsInvalidOrderException() {
        Order pending = Order.create(EVENT_ID, CUSTOMER_ID, QUANTITY);

        assertThatThrownBy(pending::confirm)
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Cannot confirm");
    }

    @Test
    void confirm_withExpiredReservation_throwsInvalidOrderException() {
        OrderId id = OrderId.generate();
        Order expired = Order.reconstitute(id, EVENT_ID, CUSTOMER_ID, QUANTITY,
                OrderStatus.RESERVED, LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusMinutes(5), List.of(TicketId.generate()));

        assertThatThrownBy(expired::confirm)
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("expired");
    }
}
