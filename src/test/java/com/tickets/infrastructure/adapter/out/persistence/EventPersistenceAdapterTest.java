package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPersistenceAdapterTest {

    @Mock
    private EventDynamoDbRepository dynamoDbRepository;

    @InjectMocks
    private EventPersistenceAdapter adapter;

    @Test
    void save_delegatesToDynamoDb() {
        Event event = Event.create("Concierto", LocalDate.of(2026, 12, 15), "Lugar", 100);
        when(dynamoDbRepository.save(event)).thenReturn(Mono.just(event));

        StepVerifier.create(adapter.save(event))
                .expectNext(event)
                .verifyComplete();

        verify(dynamoDbRepository).save(event);
    }

    @Test
    void findById_delegatesToDynamoDb() {
        EventId id = EventId.generate();
        Event event = Event.reconstitute(id, "Concierto", LocalDate.of(2026, 12, 15), "Lugar", 100);
        when(dynamoDbRepository.findById(id)).thenReturn(Mono.just(event));

        StepVerifier.create(adapter.findById(id))
                .expectNext(event)
                .verifyComplete();

        verify(dynamoDbRepository).findById(id);
    }

    @Test
    void findAll_delegatesToDynamoDb() {
        Event e1 = Event.create("A", LocalDate.of(2026, 1, 1), "Lugar", 10);
        Event e2 = Event.create("B", LocalDate.of(2026, 2, 2), "Lugar", 20);
        when(dynamoDbRepository.findAll()).thenReturn(Flux.just(e1, e2));

        StepVerifier.create(adapter.findAll())
                .expectNext(e1)
                .expectNext(e2)
                .verifyComplete();

        verify(dynamoDbRepository).findAll();
    }
}
