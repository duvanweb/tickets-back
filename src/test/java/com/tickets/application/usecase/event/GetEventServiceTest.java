package com.tickets.application.usecase.event;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEventServiceTest {

    @Mock
    private EventRepository repository;

    @InjectMocks
    private GetEventService service;

    @Test
    void getById_whenEventExists_returnsEvent() {
        EventId id = EventId.generate();
        Event event = Event.reconstitute(id, "Concierto Rock",
                LocalDate.of(2026, 12, 15), "Estadio Nacional", 5000);

        when(repository.findById(id)).thenReturn(Mono.just(event));

        StepVerifier.create(service.getById(id))
                .assertNext(result -> {
                    org.assertj.core.api.Assertions.assertThat(result.id()).isEqualTo(id);
                    org.assertj.core.api.Assertions.assertThat(result.name()).isEqualTo("Concierto Rock");
                })
                .verifyComplete();
    }

    @Test
    void getById_whenEventDoesNotExist_throwsEventNotFoundException() {
        EventId id = EventId.generate();

        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.getById(id))
                .verifyError(EventNotFoundException.class);
    }
}
