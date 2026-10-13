package com.tickets.infrastructure.adapter.in.worker;

import com.tickets.application.port.in.order.ReleaseExpiredReservationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that periodically releases expired ticket reservations.
 *
 * <p>Only active in the {@code worker} profile. Runs on a fixed delay
 * (default 30 seconds) and invokes
 * {@link ReleaseExpiredReservationsUseCase#releaseExpired()}.</p>
 */
@Component
@Profile("worker")
public class ExpiredReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiredReservationScheduler.class);

    private final ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;

    public ExpiredReservationScheduler(
            ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase) {
        this.releaseExpiredReservationsUseCase = releaseExpiredReservationsUseCase;
    }

    @Scheduled(fixedDelayString = "${tickets.reservation-scheduler-delay-ms:30000}")
    public void releaseExpiredReservations() {
        releaseExpiredReservationsUseCase.releaseExpired()
                .doOnNext(count -> {
                    if (count > 0) {
                        log.info("Released {} expired ticket reservations", count);
                    }
                })
                .doOnError(e -> log.error("Failed to release expired reservations: {}",
                        e.getMessage(), e))
                .subscribe();
    }
}
