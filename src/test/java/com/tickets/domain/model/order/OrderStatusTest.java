package com.tickets.domain.model.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void containsAllExpectedStatuses() {
        OrderStatus[] statuses = OrderStatus.values();

        assertThat(statuses).containsExactlyInAnyOrder(
                OrderStatus.PENDING,
                OrderStatus.RESERVED,
                OrderStatus.PENDING_CONFIRMATION,
                OrderStatus.SOLD,
                OrderStatus.REJECTED,
                OrderStatus.EXPIRED,
                OrderStatus.COMPLIMENTARY);
    }

    @Test
    void pending_canTransitionToReservedAndRejected() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.RESERVED)).isTrue();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.REJECTED)).isTrue();
    }

    @Test
    void pending_cannotTransitionToSoldOrExpiredOrPendingConfirmation() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SOLD)).isFalse();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.EXPIRED)).isFalse();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING_CONFIRMATION)).isFalse();
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING)).isFalse();
    }

    @Test
    void reserved_canTransitionToPendingConfirmationExpiredAndSold() {
        assertThat(OrderStatus.RESERVED.canTransitionTo(OrderStatus.PENDING_CONFIRMATION)).isTrue();
        assertThat(OrderStatus.RESERVED.canTransitionTo(OrderStatus.EXPIRED)).isTrue();
        assertThat(OrderStatus.RESERVED.canTransitionTo(OrderStatus.SOLD)).isTrue();
    }

    @Test
    void reserved_cannotTransitionToRejectedOrItself() {
        assertThat(OrderStatus.RESERVED.canTransitionTo(OrderStatus.REJECTED)).isFalse();
        assertThat(OrderStatus.RESERVED.canTransitionTo(OrderStatus.RESERVED)).isFalse();
    }

    @Test
    void pendingConfirmation_canTransitionToSold() {
        assertThat(OrderStatus.PENDING_CONFIRMATION.canTransitionTo(OrderStatus.SOLD)).isTrue();
    }

    @Test
    void pendingConfirmation_cannotTransitionToOthers() {
        assertThat(OrderStatus.PENDING_CONFIRMATION.canTransitionTo(OrderStatus.RESERVED)).isFalse();
        assertThat(OrderStatus.PENDING_CONFIRMATION.canTransitionTo(OrderStatus.REJECTED)).isFalse();
        assertThat(OrderStatus.PENDING_CONFIRMATION.canTransitionTo(OrderStatus.EXPIRED)).isFalse();
    }

    @Test
    void sold_isTerminal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.SOLD.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void rejected_isTerminal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.REJECTED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void expired_isTerminal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.EXPIRED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void complimentary_isTerminal() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.COMPLIMENTARY.canTransitionTo(target)).isFalse();
        }
    }
}
