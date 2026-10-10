package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.event.Event;
import com.tickets.domain.model.event.EventId;
import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDynamoDbRepositoryTest {

    @Mock
    private DynamoDbTable<EventEntity> eventTable;

    @InjectMocks
    private EventDynamoDbRepository repository;

    private EventEntity buildEntity(String id, String name) {
        EventEntity entity = new EventEntity();
        entity.setEventId(id);
        entity.setName(name);
        entity.setEventDate("2026-12-15");
        entity.setPlace("Estadio");
        entity.setTotalCapacity(1000);
        return entity;
    }

    @Test
    void save_callsPutItemAndReturnsEvent() {
        Event event = Event.create("Concierto", LocalDate.of(2026, 12, 15), "Estadio", 1000);

        StepVerifier.create(repository.save(event))
                .assertNext(saved -> {
                    assertThat(saved.name()).isEqualTo("Concierto");
                    assertThat(saved.totalCapacity()).isEqualTo(1000);
                })
                .verifyComplete();

        verify(eventTable).putItem(any(EventEntity.class));
    }

    @Test
    void findById_whenEntityExists_returnsMappedEvent() {
        EventId id = EventId.generate();
        EventEntity entity = buildEntity(id.value().toString(), "Concierto");
        when(eventTable.getItem(any(Key.class))).thenReturn(entity);

        StepVerifier.create(repository.findById(id))
                .assertNext(event -> {
                    assertThat(event.id()).isEqualTo(id);
                    assertThat(event.name()).isEqualTo("Concierto");
                    assertThat(event.place()).isEqualTo("Estadio");
                })
                .verifyComplete();
    }

    @Test
    void findById_whenEntityNull_returnsEmpty() {
        EventId id = EventId.generate();
        when(eventTable.getItem(any(Key.class))).thenReturn(null);

        StepVerifier.create(repository.findById(id))
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_whenEntitiesExist_returnsAllMapped() {
        EventEntity e1 = buildEntity(UUID.randomUUID().toString(), "Concierto A");
        EventEntity e2 = buildEntity(UUID.randomUUID().toString(), "Concierto B");

        PageIterable<EventEntity> pageIterable = mock(PageIterable.class);
        SdkIterable<EventEntity> items = () -> List.of(e1, e2).iterator();
        when(eventTable.scan(any(ScanEnhancedRequest.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(items);

        StepVerifier.create(repository.findAll())
                .assertNext(event -> assertThat(event.name()).isEqualTo("Concierto A"))
                .assertNext(event -> assertThat(event.name()).isEqualTo("Concierto B"))
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_whenEmpty_returnsEmptyFlux() {
        PageIterable<EventEntity> pageIterable = mock(PageIterable.class);
        SdkIterable<EventEntity> items = List.<EventEntity>of()::iterator;
        when(eventTable.scan(any(ScanEnhancedRequest.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(items);

        StepVerifier.create(repository.findAll())
                .verifyComplete();
    }
}
