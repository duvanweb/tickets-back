package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.application.port.out.repositories.TicketTransitionRepository;
import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketTransition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adapter implementing {@link TicketTransitionRepository} by delegating to
 * {@link TicketTransitionDynamoDbRepository}.
 */
@Component
public class TicketTransitionPersistenceAdapter implements TicketTransitionRepository {

    private final TicketTransitionDynamoDbRepository repository;

    public TicketTransitionPersistenceAdapter(TicketTransitionDynamoDbRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Void> saveAll(List<TicketTransition> transitions) {
        return repository.saveAll(transitions);
    }

    @Override
    public Flux<TicketTransition> findByTicketId(TicketId ticketId) {
        return repository.findByTicketId(ticketId);
    }
}
