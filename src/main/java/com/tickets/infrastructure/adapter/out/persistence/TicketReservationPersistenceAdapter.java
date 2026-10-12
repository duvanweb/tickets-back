package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.application.port.out.repositories.TicketReservationRepository;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adapter that implements the {@link TicketReservationRepository} port by delegating
 * to {@link TicketReservationDynamoDbRepository}.
 */
@Component
public class TicketReservationPersistenceAdapter implements TicketReservationRepository {

    private final TicketReservationDynamoDbRepository dynamoDbRepository;

    public TicketReservationPersistenceAdapter(TicketReservationDynamoDbRepository dynamoDbRepository) {
        this.dynamoDbRepository = dynamoDbRepository;
    }

    @Override
    public Mono<ReservationResult> reserveTicketsAtomically(EventId eventId,
                                                             int quantity,
                                                             List<Ticket> reservedTickets) {
        return dynamoDbRepository.reserveTicketsAtomically(eventId, quantity, reservedTickets);
    }
}
