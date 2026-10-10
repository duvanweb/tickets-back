package com.tickets.infrastructure.adapter.in.web;

import com.tickets.application.port.in.event.CreateEventUseCase;
import com.tickets.application.port.in.event.GetEventUseCase;
import com.tickets.application.port.in.event.ListEventsUseCase;
import com.tickets.application.usecase.event.CreateEventCommand;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.infrastructure.adapter.in.web.dto.event.CreateEventRequest;
import com.tickets.infrastructure.adapter.in.web.dto.event.EventDtoMapper;
import com.tickets.infrastructure.adapter.in.web.dto.event.EventResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for event operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/events} — create an event (201 Created)</li>
 *   <li>{@code GET /api/events/{id}} — get an event by ID (200 OK / 404)</li>
 *   <li>{@code GET /api/events} — list all events (200 OK)</li>
 * </ul></p>
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final CreateEventUseCase createEventUseCase;
    private final GetEventUseCase getEventUseCase;
    private final ListEventsUseCase listEventsUseCase;

    public EventController(CreateEventUseCase createEventUseCase,
                           GetEventUseCase getEventUseCase,
                           ListEventsUseCase listEventsUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.getEventUseCase = getEventUseCase;
        this.listEventsUseCase = listEventsUseCase;
    }

    @PostMapping
    public Mono<ResponseEntity<EventResponse>> create(@Valid @RequestBody Mono<CreateEventRequest> request) {
        return request
                .map(EventDtoMapper::toCommand)
                .flatMap(createEventUseCase::create)
                .map(event -> ResponseEntity
                        .created(java.net.URI.create("/api/events/" + event.id().value()))
                        .body(EventDtoMapper.toResponse(event)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<EventResponse>> getById(@PathVariable String id) {
        return getEventUseCase.getById(EventId.of(id))
                .map(event -> ResponseEntity.ok(EventDtoMapper.toResponse(event)));
    }

    @GetMapping
    public Flux<EventResponse> getAll() {
        return listEventsUseCase.getAll()
                .map(EventDtoMapper::toResponse);
    }
}
