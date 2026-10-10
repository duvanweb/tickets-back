package com.tickets.application.usecase.event;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.model.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEventServiceTest {

    @Mock
    private EventRepository repository;

    @InjectMocks
    private CreateEventService service;

    @Test
    void create_withValidCommand_savesAndReturnsEvent() {
        CreateEventCommand command = new CreateEventCommand(
                "Concierto Rock", LocalDate.of(2026, 12, 15), "Estadio Nacional", 5000);

        when(repository.save(any(Event.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.create(command))
                .assertNext(event -> {
                    org.assertj.core.api.Assertions.assertThat(event.id()).isNotNull();
                    org.assertj.core.api.Assertions.assertThat(event.name()).isEqualTo("Concierto Rock");
                    org.assertj.core.api.Assertions.assertThat(event.date()).isEqualTo(LocalDate.of(2026, 12, 15));
                    org.assertj.core.api.Assertions.assertThat(event.place()).isEqualTo("Estadio Nacional");
                    org.assertj.core.api.Assertions.assertThat(event.totalCapacity()).isEqualTo(5000);
                })
                .verifyComplete();
    }

    @Test
    void create_withInvalidCommand_throwsInvalidEventException() {
        CreateEventCommand command = new CreateEventCommand(
                "", LocalDate.of(2026, 12, 15), "Estadio Nacional", 5000);

        StepVerifier.create(service.create(command))
                .verifyError(com.tickets.domain.exception.InvalidEventException.class);
    }
}
