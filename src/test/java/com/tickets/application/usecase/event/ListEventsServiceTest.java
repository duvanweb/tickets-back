package com.tickets.application.usecase.event;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.model.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEventsServiceTest {

    @Mock
    private EventRepository repository;

    @InjectMocks
    private ListEventsService service;

    @Test
    void getAll_whenEventsExist_returnsAllEvents() {
        Event event1 = Event.create("Concierto A", LocalDate.of(2026, 12, 15), "Lugar A", 1000);
        Event event2 = Event.create("Concierto B", LocalDate.of(2027, 1, 20), "Lugar B", 2000);

        when(repository.findAll()).thenReturn(Flux.just(event1, event2));

        StepVerifier.create(service.getAll())
                .assertNext(event -> {
                    org.assertj.core.api.Assertions.assertThat(event.name()).isEqualTo("Concierto A");
                })
                .assertNext(event -> {
                    org.assertj.core.api.Assertions.assertThat(event.name()).isEqualTo("Concierto B");
                })
                .verifyComplete();
    }

    @Test
    void getAll_whenNoEvents_returnsEmptyFlux() {
        when(repository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(service.getAll())
                .verifyComplete();
    }
}
