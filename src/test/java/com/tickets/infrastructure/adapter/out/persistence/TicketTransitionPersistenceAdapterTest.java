package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.domain.model.ticket.TicketTransition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketTransitionPersistenceAdapterTest {

    @Mock
    private TicketTransitionDynamoDbRepository dynamoDbRepository;

    @InjectMocks
    private TicketTransitionPersistenceAdapter adapter;

    @Test
    void saveAll_delegatesToDynamoDb() {
        TicketId ticketId = TicketId.generate();
        List<TicketTransition> transitions = List.of(
                TicketTransition.create(ticketId, TicketStatus.AVAILABLE, TicketStatus.RESERVED, "order-1"),
                TicketTransition.create(ticketId, TicketStatus.RESERVED, TicketStatus.SOLD, "order-1"));
        when(dynamoDbRepository.saveAll(transitions)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.saveAll(transitions))
                .verifyComplete();

        verify(dynamoDbRepository).saveAll(transitions);
    }

    @Test
    void findByTicketId_delegatesToDynamoDb() {
        TicketId ticketId = TicketId.generate();
        TicketTransition transition = TicketTransition.create(
                ticketId, TicketStatus.AVAILABLE, TicketStatus.RESERVED, "order-1");
        when(dynamoDbRepository.findByTicketId(ticketId)).thenReturn(Flux.just(transition));

        StepVerifier.create(adapter.findByTicketId(ticketId))
                .expectNext(transition)
                .verifyComplete();

        verify(dynamoDbRepository).findByTicketId(ticketId);
    }
}
