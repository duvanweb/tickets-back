package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.ticket.Ticket;
import com.tickets.domain.model.ticket.TicketId;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.tickets.infrastructure.adapter.out.persistence.mapper.TicketPersistenceMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;

/**
 * DynamoDB operations for {@link Ticket} using the Enhanced Client.
 *
 * <p>All blocking DynamoDB calls are wrapped in {@code Mono.fromCallable} /
 * {@code Flux.fromIterable} and subscribed on {@code Schedulers.boundedElastic()}
 * to avoid blocking the Netty event loop. Batch writes are chunked into lots
 * of 25 (DynamoDB limit per batch).</p>
 */
@Component
public class TicketDynamoDbRepository {

    private static final int BATCH_SIZE = 25;

    private final DynamoDbTable<TicketEntity> ticketTable;
    private final DynamoDbEnhancedClient enhancedClient;

    public TicketDynamoDbRepository(DynamoDbTable<TicketEntity> ticketTable,
                                    DynamoDbEnhancedClient enhancedClient) {
        this.ticketTable = ticketTable;
        this.enhancedClient = enhancedClient;
    }

    /**
     * Batch-saves a list of tickets to DynamoDB.
     *
     * <p>Tickets are chunked into batches of 25 (the DynamoDB limit per
     * {@code BatchWriteItem} request) and written sequentially.</p>
     *
     * @param tickets the domain tickets to persist
     * @return a {@link Flux} emitting all saved tickets
     */
    public Flux<Ticket> saveAll(List<Ticket> tickets) {
        return Mono.fromCallable(() -> {
                    List<TicketEntity> entities = tickets.stream()
                            .map(TicketPersistenceMapper::toEntity)
                            .toList();
                    for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
                        List<TicketEntity> batch = entities.subList(
                                i, Math.min(i + BATCH_SIZE, entities.size()));
                        WriteBatch.Builder<TicketEntity> batchBuilder = WriteBatch.builder(TicketEntity.class)
                                .mappedTableResource(ticketTable);
                        for (TicketEntity entity : batch) {
                            batchBuilder.addPutItem(entity);
                        }
                        BatchWriteItemEnhancedRequest request = BatchWriteItemEnhancedRequest.builder()
                                .addWriteBatch(batchBuilder.build())
                                .build();
                        enhancedClient.batchWriteItem(request);
                    }
                    return tickets;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Finds a ticket by its ID.
     *
     * @param id the ticket ID
     * @return a {@link Mono} emitting the ticket, or empty if not found
     */
    public Mono<Ticket> findById(TicketId id) {
        return Mono.fromCallable(() -> {
                    TicketEntity entity = ticketTable.getItem(Key.builder()
                            .partitionValue(id.value().toString())
                            .build());
                    return entity != null ? TicketPersistenceMapper.toDomain(entity) : null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
