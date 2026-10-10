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
}
