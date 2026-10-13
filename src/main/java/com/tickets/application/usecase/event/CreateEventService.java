package com.tickets.application.usecase.event;
import com.tickets.application.port.in.event.CreateEventUseCase.CreateEventCommand;

import com.tickets.application.port.in.event.CreateEventUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.application.port.out.repositories.TicketRepository;
import com.tickets.domain.exception.InvalidEventException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.ticket.Ticket;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Implementation of {@link CreateEventUseCase}.
 *
 * <p>Creates a new {@link Event} via the domain factory, checks that no
 * event with the same name already exists, persists it through the
 * {@link EventRepository} port, and then generates N {@link Ticket} rows
 * (one per unit of capacity) in {@code AVAILABLE} status, batch-saving them
 * through the {@link TicketRepository} port.</p>
 */
public final class CreateEventService implements CreateEventUseCase {

    private final EventRepository repository;
    private final TicketRepository ticketRepository;

    public CreateEventService(EventRepository repository, TicketRepository ticketRepository) {
        this.repository = repository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Mono<Event> create(CreateEventCommand command) {
        return Mono.fromCallable(() ->
                Event.create(command.name(), command.date(), command.place(), command.totalCapacity()))
                .flatMap(event -> repository.existsByName(event.name())
                        .flatMap(exists -> exists
                                ? Mono.error(new InvalidEventException(
                                        "Event with name '" + event.name() + "' already exists"))
                                : repository.save(event)
                                        .flatMap(savedEvent -> {
                                            List<Ticket> tickets = generateTickets(savedEvent);
                                            return ticketRepository.saveAll(tickets)
                                                    .then(Mono.just(savedEvent));
                                        })));
    }

    private List<Ticket> generateTickets(Event event) {
        return IntStream.rangeClosed(1, event.totalCapacity())
                .mapToObj(i -> Ticket.create(event.id(), String.valueOf(i)))
                .toList();
    }
}
