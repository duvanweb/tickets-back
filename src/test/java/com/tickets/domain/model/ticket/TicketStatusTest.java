package com.tickets.domain.model.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTest {

    @Test
    void available_canTransitionToReservedAndComplimentary() {
        assertThat(TicketStatus.AVAILABLE.canTransitionTo(TicketStatus.RESERVED)).isTrue();
        assertThat(TicketStatus.AVAILABLE.canTransitionTo(TicketStatus.COMPLIMENTARY)).isTrue();
    }

    @Test
    void available_cannotTransitionToPendingConfirmationOrSold() {
        assertThat(TicketStatus.AVAILABLE.canTransitionTo(TicketStatus.PENDING_CONFIRMATION)).isFalse();
        assertThat(TicketStatus.AVAILABLE.canTransitionTo(TicketStatus.SOLD)).isFalse();
        assertThat(TicketStatus.AVAILABLE.canTransitionTo(TicketStatus.AVAILABLE)).isFalse();
    }

    @Test
    void reserved_canTransitionToPendingConfirmationAndAvailable() {
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.PENDING_CONFIRMATION)).isTrue();
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.AVAILABLE)).isTrue();
    }

    @Test
    void reserved_cannotTransitionToSoldOrComplimentary() {
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.SOLD)).isFalse();
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.COMPLIMENTARY)).isFalse();
        assertThat(TicketStatus.RESERVED.canTransitionTo(TicketStatus.RESERVED)).isFalse();
    }

    @Test
    void pendingConfirmation_canTransitionToSoldAndAvailable() {
        assertThat(TicketStatus.PENDING_CONFIRMATION.canTransitionTo(TicketStatus.SOLD)).isTrue();
        assertThat(TicketStatus.PENDING_CONFIRMATION.canTransitionTo(TicketStatus.AVAILABLE)).isTrue();
    }

    @Test
    void pendingConfirmation_cannotTransitionToReservedOrComplimentary() {
        assertThat(TicketStatus.PENDING_CONFIRMATION.canTransitionTo(TicketStatus.RESERVED)).isFalse();
        assertThat(TicketStatus.PENDING_CONFIRMATION.canTransitionTo(TicketStatus.COMPLIMENTARY)).isFalse();
        assertThat(TicketStatus.PENDING_CONFIRMATION.canTransitionTo(TicketStatus.PENDING_CONFIRMATION)).isFalse();
    }

    @Test
    void sold_isTerminal() {
        for (TicketStatus target : TicketStatus.values()) {
            assertThat(TicketStatus.SOLD.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void complimentary_isTerminal() {
        for (TicketStatus target : TicketStatus.values()) {
            assertThat(TicketStatus.COMPLIMENTARY.canTransitionTo(target)).isFalse();
        }
    }
}
