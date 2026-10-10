package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketDynamoDbRepositoryTest {

    @Mock
    private DynamoDbTable<TicketEntity> ticketTable;

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @InjectMocks
    private TicketDynamoDbRepository repository;

    private TicketEntity buildEntity(String ticketId) {
        TicketEntity entity = new TicketEntity();
        entity.setTicketId(ticketId);
        entity.setEventId(EventId.generate().value().toString());
        entity.setStatus("AVAILABLE");
        entity.setSeatNumber("A-1");
        entity.setVersion(0);
        return entity;
    }

    @Test
    void saveAll_withTickets_callsBatchWriteItemAndReturnsAllTickets() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"),
                Ticket.create(eventId, "3"));

        when(ticketTable.tableSchema()).thenReturn(TableSchema.fromBean(TicketEntity.class));

        StepVerifier.create(repository.saveAll(tickets))
                .expectNextCount(3)
                .verifyComplete();

        verify(enhancedClient, times(1)).batchWriteItem(any(BatchWriteItemEnhancedRequest.class));
    }

    @Test
    void saveAll_withMoreThan25Tickets_callsBatchWriteItemMultipleTimes() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = java.util.stream.IntStream.rangeClosed(1, 30)
                .mapToObj(i -> Ticket.create(eventId, String.valueOf(i)))
                .toList();

        when(ticketTable.tableSchema()).thenReturn(TableSchema.fromBean(TicketEntity.class));

        StepVerifier.create(repository.saveAll(tickets))
                .expectNextCount(30)
                .verifyComplete();

        verify(enhancedClient, times(2)).batchWriteItem(any(BatchWriteItemEnhancedRequest.class));
    }

    @Test
    void saveAll_withEmptyList_returnsEmptyFlux() {
        StepVerifier.create(repository.saveAll(List.of()))
                .verifyComplete();

        verifyNoInteractions(enhancedClient);
    }

    @Test
    void findById_whenEntityExists_returnsMappedTicket() {
        TicketId id = TicketId.generate();
        TicketEntity entity = buildEntity(id.value().toString());
        when(ticketTable.getItem(any(Key.class))).thenReturn(entity);

        StepVerifier.create(repository.findById(id))
                .assertNext(ticket -> {
                    assertThat(ticket.id()).isEqualTo(id);
                    assertThat(ticket.status().name()).isEqualTo("AVAILABLE");
                })
                .verifyComplete();
    }

    @Test
    void findById_whenEntityNull_returnsEmpty() {
        TicketId id = TicketId.generate();
        when(ticketTable.getItem(any(Key.class))).thenReturn(null);

        StepVerifier.create(repository.findById(id))
                .verifyComplete();
    }
}
