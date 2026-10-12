package com.tickets.application.usecase.event;
import com.tickets.application.port.in.event.CreateEventUseCase.CreateEventCommand;

import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.ticket.Ticket;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateEventServiceTest {

    @Mock
    private EventRepository repository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private CreateEventService service;

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 12, 15, 19, 30);

    @Test
    void create_withValidCommand_savesEventAndGeneratesTickets() {
        CreateEventCommand command = new CreateEventCommand(
                "Concierto Rock", DATE, "Estadio Nacional", 5000);

        when(repository.existsByName("Concierto Rock")).thenReturn(Mono.just(false));
        when(repository.save(any(Event.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));
        when(ticketRepository.saveAll(any())).thenAnswer(invocation ->
                Flux.fromIterable(invocation.getArgument(0)));

        StepVerifier.create(service.create(command))
                .assertNext(event -> {
                    assertThat(event.id()).isNotNull();
                    assertThat(event.name()).isEqualTo("Concierto Rock");
                    assertThat(event.date()).isEqualTo(DATE);
                    assertThat(event.place()).isEqualTo("Estadio Nacional");
                    assertThat(event.totalCapacity()).isEqualTo(5000);
                    assertThat(event.available()).isEqualTo(5000);
                    assertThat(event.reserved()).isZero();
                    assertThat(event.sold()).isZero();
                    assertThat(event.complimentary()).isZero();
                })
                .verifyComplete();

        verify(ticketRepository).saveAll(any());
    }

    @Test
    void create_withValidCommand_generatesCorrectNumberOfTickets() {
        CreateEventCommand command = new CreateEventCommand(
                "Concierto Rock", DATE, "Estadio Nacional", 3);

        when(repository.existsByName("Concierto Rock")).thenReturn(Mono.just(false));
        when(repository.save(any(Event.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));
        when(ticketRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Ticket> tickets = invocation.getArgument(0);
            assertThat(tickets).hasSize(3);
            assertThat(tickets.get(0).seatNumber()).isEqualTo("1");
            assertThat(tickets.get(1).seatNumber()).isEqualTo("2");
            assertThat(tickets.get(2).seatNumber()).isEqualTo("3");
            return Flux.fromIterable(tickets);
        });

        StepVerifier.create(service.create(command))
                .assertNext(event -> assertThat(event.totalCapacity()).isEqualTo(3))
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

        verifyNoInteractions(ticketRepository);
    }
}
