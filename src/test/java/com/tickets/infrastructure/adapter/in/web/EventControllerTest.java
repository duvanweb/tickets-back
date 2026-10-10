package com.tickets.infrastructure.adapter.in.web;

import com.tickets.application.port.in.event.CreateEventUseCase;
import com.tickets.application.port.in.event.GetEventUseCase;
import com.tickets.application.port.in.event.ListEventsUseCase;
import com.tickets.application.usecase.event.CreateEventCommand;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private CreateEventUseCase createEventUseCase;

    @Mock
    private GetEventUseCase getEventUseCase;

    @Mock
    private ListEventsUseCase listEventsUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        EventController controller = new EventController(createEventUseCase, getEventUseCase, listEventsUseCase);
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String VALID_BODY = """
            {"name": "Concierto Rock", "date": "2026-12-15", "place": "Estadio Nacional", "totalCapacity": 5000}""";

    @Test
    void create_withValidBody_returns201() {
        Event event = Event.create("Concierto Rock", LocalDate.of(2026, 12, 15), "Estadio Nacional", 5000);
        when(createEventUseCase.create(any(CreateEventCommand.class))).thenReturn(Mono.just(event));

        webTestClient.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Concierto Rock")
                .jsonPath("$.date").isEqualTo("2026-12-15")
                .jsonPath("$.place").isEqualTo("Estadio Nacional")
                .jsonPath("$.totalCapacity").isEqualTo(5000)
                .jsonPath("$.id").isNotEmpty();
    }

    @Test
    void create_withBlankName_returns400() {
        webTestClient.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "", "date": "2026-12-15", "place": "Estadio Nacional", "totalCapacity": 5000}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_withNullDate_returns400() {
        webTestClient.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Concierto", "date": null, "place": "Estadio", "totalCapacity": 100}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_withBlankPlace_returns400() {
        webTestClient.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Concierto", "date": "2026-12-15", "place": "", "totalCapacity": 100}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_withZeroCapacity_returns400() {
        webTestClient.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Concierto", "date": "2026-12-15", "place": "Estadio", "totalCapacity": 0}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getById_whenExists_returns200() {
        EventId id = EventId.generate();
        Event event = Event.reconstitute(id, "Concierto Rock", LocalDate.of(2026, 12, 15), "Estadio", 1000);
        when(getEventUseCase.getById(id)).thenReturn(Mono.just(event));

        webTestClient.get().uri("/api/events/{id}", id.value().toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.value().toString())
                .jsonPath("$.name").isEqualTo("Concierto Rock");
    }

    @Test
    void getById_whenNotFound_returns404() {
        EventId id = EventId.generate();
        when(getEventUseCase.getById(id)).thenReturn(Mono.error(new EventNotFoundException("not found")));

        webTestClient.get().uri("/api/events/{id}", id.value().toString())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("not found");
    }

    @Test
    void getAll_whenEventsExist_returns200() {
        Event e1 = Event.create("Concierto A", LocalDate.of(2026, 12, 15), "Lugar A", 1000);
        Event e2 = Event.create("Concierto B", LocalDate.of(2027, 1, 20), "Lugar B", 2000);
        when(listEventsUseCase.getAll()).thenReturn(Flux.just(e1, e2));

        webTestClient.get().uri("/api/events")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Concierto A")
                .jsonPath("$[1].name").isEqualTo("Concierto B");
    }

    @Test
    void getAll_whenEmpty_returns200EmptyArray() {
        when(listEventsUseCase.getAll()).thenReturn(Flux.empty());

        webTestClient.get().uri("/api/events")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }
}
