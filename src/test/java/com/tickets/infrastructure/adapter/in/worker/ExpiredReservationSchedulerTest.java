package com.tickets.infrastructure.adapter.in.worker;

import com.tickets.application.port.in.order.ReleaseExpiredReservationsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredReservationSchedulerTest {

    @Mock
    private ReleaseExpiredReservationsUseCase useCase;

    private ExpiredReservationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ExpiredReservationScheduler(useCase);
    }

    @Test
    void releaseExpiredReservations_invokesUseCase() {
        when(useCase.releaseExpired()).thenReturn(Mono.just(3));

        scheduler.releaseExpiredReservations();

        verify(useCase).releaseExpired();
    }

    @Test
    void releaseExpiredReservations_whenUseCaseErrors_doesNotPropagate() {
        when(useCase.releaseExpired()).thenReturn(Mono.error(new RuntimeException("DB error")));

        scheduler.releaseExpiredReservations();

        verify(useCase).releaseExpired();
    }

    @Test
    void releaseExpiredReservations_whenZeroExpired_doesNothing() {
        when(useCase.releaseExpired()).thenReturn(Mono.just(0));

        scheduler.releaseExpiredReservations();

        verify(useCase).releaseExpired();
    }
}
