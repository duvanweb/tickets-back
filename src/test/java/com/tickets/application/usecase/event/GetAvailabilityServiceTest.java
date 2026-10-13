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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAvailabilityServiceTest {

    @Mock
    private EventRepository repository;

    @InjectMocks
    private GetAvailabilityService service;

    @Test
    void getAvailability_whenEventExists_returnsAvailabilityWithCounters() {
        EventId id = EventId.generate();
        Event event = Event.reconstitute(id, "Concierto Rock",
                LocalDateTime.of(2026, 12, 15, 19, 30), "Estadio Nacional", 5000,
                3000, 1000, 800, 200);

        when(repository.findById(id)).thenReturn(Mono.just(event));

        StepVerifier.create(service.getAvailability(id))
                .assertNext(availability -> {
                    assertThat(availability.eventId()).isEqualTo(id);
                    assertThat(availability.totalCapacity()).isEqualTo(5000);
                    assertThat(availability.available()).isEqualTo(3000);
                    assertThat(availability.reserved()).isEqualTo(1000);
                    assertThat(availability.sold()).isEqualTo(800);
                    assertThat(availability.complimentary()).isEqualTo(200);
                })
                .verifyComplete();
    }

    @Test
    void getAvailability_whenEventDoesNotExist_throwsEventNotFoundException() {
        EventId id = EventId.generate();

        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.getAvailability(id))
                .verifyError(EventNotFoundException.class);
    }
}
