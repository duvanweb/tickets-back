package com.tickets.application.usecase.event;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.model.event.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEventServiceTest {

    @Mock
    private EventRepository repository;

    @InjectMocks
    private CreateEventService service;

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 12, 15, 19, 30);

    @Test
    void create_withValidCommand_savesAndReturnsEvent() {
        CreateEventCommand command = new CreateEventCommand(
                "Concierto Rock", DATE, "Estadio Nacional", 5000);

        when(repository.existsByName("Concierto Rock")).thenReturn(Mono.just(false));
        when(repository.save(any(Event.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.create(command))
                .assertNext(event -> {
                    assertThat(event.id()).isNotNull();
                    assertThat(event.name()).isEqualTo("Concierto Rock");
                    assertThat(event.date()).isEqualTo(DATE);
                    assertThat(event.place()).isEqualTo("Estadio Nacional");
                    assertThat(event.totalCapacity()).isEqualTo(5000);
                })
                .verifyComplete();
    }

    @Test
    void create_withInvalidCommand_throwsInvalidEventException() {
        CreateEventCommand command = new CreateEventCommand(
                "", DATE, "Estadio Nacional", 5000);

        StepVerifier.create(service.create(command))
                .verifyError(InvalidEventException.class);
    }

    @Test
    void create_whenNameAlreadyExists_throwsInvalidEventException() {
        CreateEventCommand command = new CreateEventCommand(
                "Concierto Rock", DATE, "Estadio Nacional", 5000);

        when(repository.existsByName("Concierto Rock")).thenReturn(Mono.just(true));

        StepVerifier.create(service.create(command))
                .verifyErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(InvalidEventException.class);
                    assertThat(error.getMessage()).contains("already exists");
                });
    }
}
