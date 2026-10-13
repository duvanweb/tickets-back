package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketPersistenceAdapterTest {

    @Mock
    private TicketDynamoDbRepository dynamoDbRepository;

    @InjectMocks
    private TicketPersistenceAdapter adapter;

    @Test
    void saveAll_delegatesToDynamoDb() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"));
        when(dynamoDbRepository.saveAll(tickets)).thenReturn(Flux.fromIterable(tickets));

        StepVerifier.create(adapter.saveAll(tickets))
                .expectNextCount(2)
                .verifyComplete();

        verify(dynamoDbRepository).saveAll(tickets);
    }

    @Test
    void findById_delegatesToDynamoDb() {
        TicketId id = TicketId.generate();
        Ticket ticket = Ticket.create(EventId.generate(), "A-1");
        when(dynamoDbRepository.findById(id)).thenReturn(Mono.just(ticket));

        StepVerifier.create(adapter.findById(id))
                .expectNext(ticket)
                .verifyComplete();

        verify(dynamoDbRepository).findById(id);
    }

    @Test
    void findAvailableByEvent_delegatesToDynamoDb() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"));
        when(dynamoDbRepository.findAvailableByEvent(eventId, 5)).thenReturn(Flux.fromIterable(tickets));

        StepVerifier.create(adapter.findAvailableByEvent(eventId, 5))
                .expectNextCount(2)
                .verifyComplete();

        verify(dynamoDbRepository).findAvailableByEvent(eventId, 5);
    }

    @Test
    void findExpiredReservations_delegatesToDynamoDb() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"));
        LocalDateTime cutoff = LocalDateTime.now();
        when(dynamoDbRepository.findExpiredReservations(cutoff)).thenReturn(Flux.fromIterable(tickets));

        StepVerifier.create(adapter.findExpiredReservations(cutoff))
                .expectNextCount(2)
                .verifyComplete();

        verify(dynamoDbRepository).findExpiredReservations(cutoff);
    }
}
