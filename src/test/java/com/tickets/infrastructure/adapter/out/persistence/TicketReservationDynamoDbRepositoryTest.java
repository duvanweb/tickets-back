package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketReservationDynamoDbRepositoryTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private DynamoDbTable<EventEntity> eventTable;

    @Mock
    private DynamoDbTable<TicketEntity> ticketTable;

    @Mock
    private TableSchema<TicketEntity> ticketTableSchema;

    private TicketReservationDynamoDbRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TicketReservationDynamoDbRepository(dynamoDbClient, eventTable, ticketTable);
    }

    private void stubTableNames() {
        when(eventTable.tableName()).thenReturn("events");
        when(ticketTable.tableName()).thenReturn("tickets");
        when(ticketTable.tableSchema()).thenReturn(ticketTableSchema);
        when(ticketTableSchema.itemToMap(any(), anyBoolean())).thenReturn(
                Map.of("ticketId", AttributeValue.builder().s("t1").build()));
    }

    @Test
    void reserveTicketsAtomically_whenAllConditionsPass_returnsSuccess() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(
                Ticket.create(eventId, "1"),
                Ticket.create(eventId, "2"));

        stubTableNames();

        StepVerifier.create(repository.reserveTicketsAtomically(eventId, 2, tickets))
                .assertNext(result -> assertThat(result).isInstanceOf(ReservationResult.Success.class))
                .verifyComplete();

        verify(dynamoDbClient, times(1)).transactWriteItems(any(TransactWriteItemsRequest.class));
    }

    @Test
    void reserveTicketsAtomically_whenEventConditionFails_returnsEventConditionFailed() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(Ticket.create(eventId, "1"));

        stubTableNames();

        TransactionCanceledException exception = TransactionCanceledException.builder()
                .cancellationReasons(
                        CancellationReason.builder().code("ConditionalCheckFailed").build(),
                        CancellationReason.builder().code("None").build())
                .build();
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
                .thenThrow(exception);

        StepVerifier.create(repository.reserveTicketsAtomically(eventId, 1, tickets))
                .assertNext(result -> assertThat(result).isInstanceOf(ReservationResult.EventConditionFailed.class))
                .verifyComplete();
    }

    @Test
    void reserveTicketsAtomically_whenTicketConditionFails_returnsTicketConditionFailed() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(Ticket.create(eventId, "1"));

        stubTableNames();

        TransactionCanceledException exception = TransactionCanceledException.builder()
                .cancellationReasons(
                        CancellationReason.builder().code("None").build(),
                        CancellationReason.builder().code("ConditionalCheckFailed").build())
                .build();
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
                .thenThrow(exception);

        StepVerifier.create(repository.reserveTicketsAtomically(eventId, 1, tickets))
                .assertNext(result -> assertThat(result).isInstanceOf(ReservationResult.TicketConditionFailed.class))
                .verifyComplete();
    }

    @Test
    void reserveTicketsAtomically_whenBothConditionsFail_prioritizesEventConditionFailed() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(Ticket.create(eventId, "1"));

        stubTableNames();

        TransactionCanceledException exception = TransactionCanceledException.builder()
                .cancellationReasons(
                        CancellationReason.builder().code("ConditionalCheckFailed").build(),
                        CancellationReason.builder().code("ConditionalCheckFailed").build())
                .build();
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
                .thenThrow(exception);

        StepVerifier.create(repository.reserveTicketsAtomically(eventId, 1, tickets))
                .assertNext(result -> assertThat(result).isInstanceOf(ReservationResult.EventConditionFailed.class))
                .verifyComplete();
    }

    @Test
    void reserveTicketsAtomically_withCorrectRequestStructure() {
        EventId eventId = EventId.generate();
        List<Ticket> tickets = List.of(Ticket.create(eventId, "1"), Ticket.create(eventId, "2"));

        stubTableNames();

        StepVerifier.create(repository.reserveTicketsAtomically(eventId, 2, tickets))
                .assertNext(result -> assertThat(result).isInstanceOf(ReservationResult.Success.class))
                .verifyComplete();

        var captor = org.mockito.ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
        verify(dynamoDbClient).transactWriteItems(captor.capture());
        TransactWriteItemsRequest request = captor.getValue();

        // 1 Event update + 2 ticket puts = 3 transact items
        assertThat(request.transactItems()).hasSize(3);

        // First item is the Event update
        assertThat(request.transactItems().get(0).update()).isNotNull();
        assertThat(request.transactItems().get(0).update().tableName()).isEqualTo("events");
        assertThat(request.transactItems().get(0).update().updateExpression())
                .contains("available = available - :qty");
        assertThat(request.transactItems().get(0).update().conditionExpression())
                .isEqualTo("available >= :qty");

        // Items 1 and 2 are ticket puts
        assertThat(request.transactItems().get(1).put()).isNotNull();
        assertThat(request.transactItems().get(1).put().tableName()).isEqualTo("tickets");
        assertThat(request.transactItems().get(1).put().conditionExpression())
                .isEqualTo("#st = :available");
        assertThat(request.transactItems().get(2).put()).isNotNull();
    }
}
