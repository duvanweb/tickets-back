package com.tickets.application.port.out.repositories;

import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Secondary port (repository interface) for ticket persistence.
 */
public interface TicketRepository {

    Flux<Ticket> saveAll(List<Ticket> tickets);

    Mono<Ticket> findById(TicketId id);
}
