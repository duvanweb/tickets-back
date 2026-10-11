package com.tickets.application.usecase.event;

import com.tickets.application.port.in.event.GetAvailabilityUseCase;
import com.tickets.application.port.out.repositories.EventRepository;
import com.tickets.domain.exception.EventNotFoundException;
import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link GetAvailabilityUseCase}.
 *
 * <p>Loads the event and maps its counters to an {@link EventAvailability} read model.</p>
 */
public final class GetAvailabilityService implements GetAvailabilityUseCase {

    private final EventRepository repository;

    public GetAvailabilityService(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<EventAvailability> getAvailability(EventId id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new EventNotFoundException(
                        "Event not found with id: " + id)))
                .map(this::toAvailability);
    }

    private EventAvailability toAvailability(Event event) {
        return new EventAvailability(
                event.id(),
                event.totalCapacity(),
                event.available(),
                event.reserved(),
                event.sold(),
                event.complimentary());
    }
}
