package com.tickets.application.port.in.order;

import reactor.core.publisher.Mono;

/**
 * Use case for releasing expired ticket reservations.
 *
 * <p>Finds tickets with {@code RESERVED} status whose reservation TTL has
 * passed, releases them back to {@code AVAILABLE}, updates event counters,
 * and transitions affected orders to {@code EXPIRED}.</p>
 */
public interface ReleaseExpiredReservationsUseCase {

    Mono<Integer> releaseExpired();
}
