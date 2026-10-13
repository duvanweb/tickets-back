package com.tickets.application.port.out.repositories;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Secondary port (repository interface) for ticket persistence.
 */
public interface TicketRepository {

    Flux<Ticket> saveAll(List<Ticket> tickets);

    Mono<Ticket> findById(TicketId id);

    /**
     * Finds available tickets for a given event, limited to {@code limit} results.
     *
     * <p>Queries the {@code event-status-index} GSI where
     * {@code eventId = eventId AND status = AVAILABLE}.</p>
     *
     * @param eventId the event ID
     * @param limit   the maximum number of tickets to return
     * @return a {@link Flux} emitting available tickets
     */
    Flux<Ticket> findAvailableByEvent(EventId eventId, int limit);

    /**
     * Finds tickets with {@code RESERVED} status whose reservation deadline
     * has passed (i.e. {@code reservedUntil < cutoff}).
     *
     * <p>Queries the {@code status-reservedUntil-index} GSI.</p>
     *
     * @param cutoff the cutoff timestamp (typically {@code now})
     * @return a {@link Flux} emitting expired reserved tickets
     */
    Flux<Ticket> findExpiredReservations(LocalDateTime cutoff);
}
