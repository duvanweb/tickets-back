package com.tickets.application.port.out.repositories;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Secondary port for atomic ticket reservation using DynamoDB TransactWriteItems.
 *
 * <p>Atomically reserves tickets and updates event counters in a single
 * DynamoDB transaction with conditional checks to prevent overselling.</p>
 */
public interface TicketReservationRepository {

    /**
     * Atomically reserves the given tickets and adjusts event counters.
     *
     * <p>Uses DynamoDB TransactWriteItems to atomically:
     * <ul>
     *   <li>Update Event counters: {@code available -= qty, reserved += qty}
     *       with condition {@code available >= :qty}</li>
     *   <li>Put each Ticket with status RESERVED
     *       with condition {@code status = :available}</li>
     * </ul></p>
     *
     * <p>If any condition fails, the entire transaction is cancelled and
     * no changes are applied. The {@link ReservationResult} indicates
     * which condition failed so the caller can decide whether to retry.</p>
     *
     * @param eventId        the event whose counters to adjust
     * @param quantity       the number of tickets being reserved
     * @param reservedTickets the domain tickets already transitioned to RESERVED
     *                        (with orderId, customerId, reservedUntil set)
     * @return a {@link Mono} emitting the {@link ReservationResult}
     */
    Mono<ReservationResult> reserveTicketsAtomically(EventId eventId,
                                                      int quantity,
                                                      List<Ticket> reservedTickets);
}
