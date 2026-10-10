package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adapter that implements the {@link TicketRepository} port by delegating
 * to {@link TicketDynamoDbRepository}.
 */
@Component
public class TicketPersistenceAdapter implements TicketRepository {

    private final TicketDynamoDbRepository dynamoDbRepository;

    public TicketPersistenceAdapter(TicketDynamoDbRepository dynamoDbRepository) {
        this.dynamoDbRepository = dynamoDbRepository;
    }

    @Override
    public Flux<Ticket> saveAll(List<Ticket> tickets) {
        return dynamoDbRepository.saveAll(tickets);
    }

    @Override
    public Mono<Ticket> findById(TicketId id) {
        return dynamoDbRepository.findById(id);
    }
}
