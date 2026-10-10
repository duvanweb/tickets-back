package com.tickets.infrastructure.adapter.out.persistence.mapper;

import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;

import java.time.LocalDateTime;

/**
 * Manual mapper between the domain {@link Event} and the persistence {@link EventEntity}.
 */
public final class EventPersistenceMapper {

    private EventPersistenceMapper() {
    }

    /**
     * Converts a domain {@link Event} to a persistence {@link EventEntity}.
     *
     * @param event the domain event
     * @return the DynamoDB entity
     */
    public static EventEntity toEntity(Event event) {
        EventEntity entity = new EventEntity();
        entity.setEventId(event.id().value().toString());
        entity.setName(event.name());
        entity.setEventDate(event.date().toString());
        entity.setPlace(event.place());
        entity.setTotalCapacity(event.totalCapacity());
        return entity;
    }

    /**
     * Converts a persistence {@link EventEntity} to a domain {@link Event}.
     *
     * @param entity the DynamoDB entity
     * @return the domain event
     */
    public static Event toDomain(EventEntity entity) {
        return Event.reconstitute(
                EventId.of(entity.getEventId()),
                entity.getName(),
                LocalDateTime.parse(entity.getEventDate()),
                entity.getPlace(),
                entity.getTotalCapacity()
        );
    }
}
