package com.tickets.application.port.out.repositories;

import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketTransition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Outbound port for persisting and querying ticket transition audit records.
 */
public interface TicketTransitionRepository {

    Mono<Void> saveAll(List<TicketTransition> transitions);

    Flux<TicketTransition> findByTicketId(TicketId ticketId);
}
