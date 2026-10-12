package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.application.port.out.repositories.ReservationResult;
import com.tickets.application.port.out.repositories.TicketReservationRepository;
import com.tickets.domain.model.event.EventId;
import com.tickets.domain.model.ticket.Ticket;
import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.tickets.infrastructure.adapter.out.persistence.mapper.TicketPersistenceMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DynamoDB implementation of {@link TicketReservationRepository} using
 * low-level {@link DynamoDbClient#transactWriteItems} for atomic conditional writes.
 *
 * <p>Uses the low-level client instead of the Enhanced Client's
 * {@code TransactWriteItemsEnhancedRequest} because the Enhanced Client
 * only supports full-item Puts within transactions, not relative
 * {@code SET} update expressions. The Event counter update requires
 * a relative expression ({@code SET available = available - :qty,
 * reserved = reserved + :qty}) to avoid lost updates.</p>
 *
 * <p>All blocking DynamoDB calls are wrapped in {@code Mono.fromCallable}
 * and subscribed on {@code Schedulers.boundedElastic()} to avoid blocking
 * the Netty event loop.</p>
 */
@Component
public class TicketReservationDynamoDbRepository implements TicketReservationRepository {

    private static final String EVENT_CONDITION_FAILED = "ConditionalCheckFailed";

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbTable<EventEntity> eventTable;
    private final DynamoDbTable<TicketEntity> ticketTable;

    public TicketReservationDynamoDbRepository(DynamoDbClient dynamoDbClient,
                                                DynamoDbTable<EventEntity> eventTable,
                                                DynamoDbTable<TicketEntity> ticketTable) {
        this.dynamoDbClient = dynamoDbClient;
        this.eventTable = eventTable;
        this.ticketTable = ticketTable;
    }

    @Override
    public Mono<ReservationResult> reserveTicketsAtomically(EventId eventId,
                                                             int quantity,
                                                             List<Ticket> reservedTickets) {
        return Mono.fromCallable(() -> doTransactWriteItems(eventId, quantity, reservedTickets))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private ReservationResult doTransactWriteItems(EventId eventId,
                                                    int quantity,
                                                    List<Ticket> reservedTickets) {
        List<TransactWriteItem> transactItems = new ArrayList<>(reservedTickets.size() + 1);

        // 1. Event Update: relative counter adjustment with condition available >= :qty
        transactItems.add(buildEventUpdateItem(eventId, quantity));

        // 2. Ticket Puts: full item with condition status = :available
        for (Ticket ticket : reservedTickets) {
            transactItems.add(buildTicketPutItem(ticket));
        }

        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(transactItems)
                .build();

        try {
            dynamoDbClient.transactWriteItems(request);
            return new ReservationResult.Success();
        } catch (TransactionCanceledException ex) {
            return interpretCancellation(ex);
        }
    }

    /**
     * Builds a TransactWriteItem for the Event with a relative update expression.
     *
     * <p>Update: {@code SET available = available - :qty, reserved = reserved + :qty}
     * Condition: {@code available >= :qty}</p>
     */
    private TransactWriteItem buildEventUpdateItem(EventId eventId, int quantity) {
        Map<String, AttributeValue> eventKey = Map.of(
                "eventId", AttributeValue.builder().s(eventId.value().toString()).build());

        Map<String, AttributeValue> expressionValues = Map.of(
                ":qty", AttributeValue.builder().n(String.valueOf(quantity)).build());

        return TransactWriteItem.builder()
                .update(builder -> builder
                        .tableName(eventTable.tableName())
                        .key(eventKey)
                        .updateExpression("SET available = available - :qty, reserved = reserved + :qty")
                        .conditionExpression("available >= :qty")
                        .expressionAttributeValues(expressionValues)
                        .build())
                .build();
    }

    /**
     * Builds a TransactWriteItem for a Ticket with a full-item Put and condition.
     *
     * <p>Put: full ticket entity with status = RESERVED
     * Condition: {@code #st = :available} (status must be AVAILABLE)</p>
     */
    private TransactWriteItem buildTicketPutItem(Ticket ticket) {
        TicketEntity entity = TicketPersistenceMapper.toEntity(ticket);
        Map<String, AttributeValue> itemMap = ticketTable.tableSchema()
                .itemToMap(entity, true);

        Map<String, String> expressionNames = Map.of("#st", "status");
        Map<String, AttributeValue> expressionValues = Map.of(
                ":available", AttributeValue.builder().s("AVAILABLE").build());

        return TransactWriteItem.builder()
                .put(builder -> builder
                        .tableName(ticketTable.tableName())
                        .item(itemMap)
                        .conditionExpression("#st = :available")
                        .expressionAttributeNames(expressionNames)
                        .expressionAttributeValues(expressionValues)
                        .build())
                .build();
    }

    /**
     * Interprets the cancellation reasons to determine which condition failed.
     *
     * <p>Index 0 = Event, indices 1..N = Tickets. Event failure takes priority
     * (it means not enough availability, which is the more fundamental issue).</p>
     */
    private ReservationResult interpretCancellation(TransactionCanceledException ex) {
        List<CancellationReason> reasons = ex.cancellationReasons();

        // Check Event condition first (index 0)
        if (!reasons.isEmpty() && isConditionalCheckFailed(reasons.get(0))) {
            return new ReservationResult.EventConditionFailed();
        }

        // Check Ticket conditions (indices 1..N)
        for (int i = 1; i < reasons.size(); i++) {
            if (isConditionalCheckFailed(reasons.get(i))) {
                return new ReservationResult.TicketConditionFailed();
            }
        }

        // Unknown cancellation reason — treat as ticket condition failed (retryable)
        return new ReservationResult.TicketConditionFailed();
    }

    private boolean isConditionalCheckFailed(CancellationReason reason) {
        return reason != null
                && reason.code() != null
                && reason.code().equals(EVENT_CONDITION_FAILED);
    }
}
