package com.tickets.infrastructure.adapter.in.web;

import com.tickets.application.port.in.event.CreateEventUseCase;
import com.tickets.application.port.in.event.GetAvailabilityUseCase;
import com.tickets.application.port.in.event.GetEventUseCase;
import com.tickets.application.port.in.event.ListEventsUseCase;
import com.tickets.application.port.in.order.GrantComplimentaryUseCase;
import com.tickets.application.usecase.event.CreateEventCommand;
import com.tickets.application.usecase.event.EventAvailability;
import com.tickets.application.usecase.order.GrantComplimentaryCommand;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.order.Order;
import com.tickets.domain.model.order.OrderId;
import com.tickets.domain.model.order.OrderStatus;
import com.tickets.domain.model.ticket.TicketId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

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

    @Mock
    private GetAvailabilityUseCase getAvailabilityUseCase;

    @Mock
    private GrantComplimentaryUseCase grantComplimentaryUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        EventController controller = new EventController(createEventUseCase, getEventUseCase,
                listEventsUseCase, getAvailabilityUseCase, grantComplimentaryUseCase);
        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String VALID_BODY = """
            {"name": "Concierto Rock", "date": "2026-12-15T19:30:00", "place": "Estadio Nacional", "totalCapacity": 5000}""";

    @Test
    void create_withValidBody_returns201() {
        Event event = Event.create("Concierto Rock", LocalDateTime.of(2026, 12, 15, 19, 30), "Estadio Nacional", 5000);
        when(createEventUseCase.create(any(CreateEventCommand.class))).thenReturn(Mono.just(event));

        webTestClient.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(VALID_BODY)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Concierto Rock")
                .jsonPath("$.date").isEqualTo("2026-12-15T19:30:00")
                .jsonPath("$.place").isEqualTo("Estadio Nacional")
                .jsonPath("$.totalCapacity").isEqualTo(5000)
                .jsonPath("$.available").isEqualTo(5000)
                .jsonPath("$.reserved").isEqualTo(0)
                .jsonPath("$.sold").isEqualTo(0)
                .jsonPath("$.complimentary").isEqualTo(0)
                .jsonPath("$.id").isNotEmpty();
    }

    @Test
    void create_withBlankName_returns400() {
        webTestClient.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "", "date": "2026-12-15T19:30:00", "place": "Estadio Nacional", "totalCapacity": 5000}""")
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
                        {"name": "Concierto", "date": "2026-12-15T19:30:00", "place": "", "totalCapacity": 100}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void create_withZeroCapacity_returns400() {
        webTestClient.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Concierto", "date": "2026-12-15T19:30:00", "place": "Estadio", "totalCapacity": 0}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getById_whenExists_returns200() {
        EventId id = EventId.generate();
        Event event = Event.reconstitute(id, "Concierto Rock", LocalDateTime.of(2026, 12, 15, 19, 30), "Estadio", 1000, 1000, 0, 0, 0);
        when(getEventUseCase.getById(id)).thenReturn(Mono.just(event));

        webTestClient.get().uri("/api/events/{id}", id.value().toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.value().toString())
                .jsonPath("$.name").isEqualTo("Concierto Rock")
                .jsonPath("$.available").isEqualTo(1000)
                .jsonPath("$.reserved").isEqualTo(0)
                .jsonPath("$.sold").isEqualTo(0)
                .jsonPath("$.complimentary").isEqualTo(0);
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
        Event e1 = Event.create("Concierto A", LocalDateTime.of(2026, 12, 15, 19, 30), "Lugar A", 1000);
        Event e2 = Event.create("Concierto B", LocalDateTime.of(2027, 1, 20, 20, 0), "Lugar B", 2000);
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

    @Test
    void getAvailability_whenEventExists_returns200() {
        EventId id = EventId.generate();
        EventAvailability availability = new EventAvailability(id, 5000, 3000, 1000, 800, 200);
        when(getAvailabilityUseCase.getAvailability(id)).thenReturn(Mono.just(availability));

        webTestClient.get().uri("/api/events/{id}/availability", id.value().toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.eventId").isEqualTo(id.value().toString())
                .jsonPath("$.totalCapacity").isEqualTo(5000)
                .jsonPath("$.available").isEqualTo(3000)
                .jsonPath("$.reserved").isEqualTo(1000)
                .jsonPath("$.sold").isEqualTo(800)
                .jsonPath("$.complimentary").isEqualTo(200);
    }

    @Test
    void getAvailability_whenNotFound_returns404() {
        EventId id = EventId.generate();
        when(getAvailabilityUseCase.getAvailability(id)).thenReturn(Mono.error(new EventNotFoundException("not found")));

        webTestClient.get().uri("/api/events/{id}/availability", id.value().toString())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("not found");
    }

    @Test
    void grantComplimentary_withValidBody_returns201() {
        EventId eventId = EventId.generate();
        OrderId orderId = OrderId.generate();
        List<TicketId> ticketIds = List.of(TicketId.generate(), TicketId.generate());
        Order order = Order.reconstitute(orderId, eventId, "vip-customer", 2,
                OrderStatus.COMPLIMENTARY, LocalDateTime.now(), null, ticketIds);
        when(grantComplimentaryUseCase.grant(any(GrantComplimentaryCommand.class))).thenReturn(Mono.just(order));

        webTestClient.post().uri("/api/events/{id}/complimentary", eventId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"customerId": "vip-customer", "quantity": 2}""")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(orderId.value().toString())
                .jsonPath("$.status").isEqualTo("COMPLIMENTARY")
                .jsonPath("$.customerId").isEqualTo("vip-customer")
                .jsonPath("$.quantity").isEqualTo(2);
    }

    @Test
    void grantComplimentary_withBlankCustomerId_returns400() {
        EventId eventId = EventId.generate();

        webTestClient.post().uri("/api/events/{id}/complimentary", eventId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"customerId": "", "quantity": 2}""")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void grantComplimentary_withZeroQuantity_returns400() {
        EventId eventId = EventId.generate();

        webTestClient.post().uri("/api/events/{id}/complimentary", eventId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"customerId": "vip-customer", "quantity": 0}""")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
