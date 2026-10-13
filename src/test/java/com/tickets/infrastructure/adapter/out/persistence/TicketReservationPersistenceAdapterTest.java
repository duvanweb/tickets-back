package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketReservationPersistenceAdapterTest {

    @Mock
    private TicketReservationDynamoDbRepository dynamoDbRepository;

    @InjectMocks
    private TicketReservationPersistenceAdapter adapter;

    @Test
    void reserveTicketsAtomically_delegatesToDynamoDb() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(Ticket.create(eventId, "1"));

        when(dynamoDbRepository.reserveTicketsAtomically(eventId, 1, tickets))
                .thenReturn(Mono.just(new ReservationResult.Success()));

        StepVerifier.create(adapter.reserveTicketsAtomically(eventId, 1, tickets))
                .assertNext(result -> assertThat(result).isInstanceOf(ReservationResult.Success.class))
                .verifyComplete();

        verify(dynamoDbRepository).reserveTicketsAtomically(eventId, 1, tickets);
    }
}
