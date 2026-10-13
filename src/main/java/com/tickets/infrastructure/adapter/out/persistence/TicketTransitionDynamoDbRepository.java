package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketTransition;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketTransitionEntity;
import com.tickets.infrastructure.adapter.out.persistence.mapper.TicketTransitionPersistenceMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;

/**
 * DynamoDB operations for {@link TicketTransition} using the Enhanced Client.
 *
 * <p>All blocking DynamoDB calls are wrapped in {@code Mono.fromCallable} and
 * subscribed on {@code Schedulers.boundedElastic()} to avoid blocking the
 * Netty event loop.</p>
 */
@Component
public class TicketTransitionDynamoDbRepository {

    private static final int BATCH_SIZE = 25;

    private final DynamoDbTable<TicketTransitionEntity> transitionTable;
    private final DynamoDbEnhancedClient enhancedClient;

    public TicketTransitionDynamoDbRepository(DynamoDbTable<TicketTransitionEntity> transitionTable,
                                              DynamoDbEnhancedClient enhancedClient) {
        this.transitionTable = transitionTable;
        this.enhancedClient = enhancedClient;
    }

    public Mono<Void> saveAll(List<TicketTransition> transitions) {
        if (transitions.isEmpty()) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> {
                    List<TicketTransitionEntity> entities = transitions.stream()
                            .map(TicketTransitionPersistenceMapper::toEntity)
                            .toList();
                    for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
                        List<TicketTransitionEntity> batch = entities.subList(
                                i, Math.min(i + BATCH_SIZE, entities.size()));
                        WriteBatch.Builder<TicketTransitionEntity> batchBuilder =
                                WriteBatch.builder(TicketTransitionEntity.class)
                                        .mappedTableResource(transitionTable);
                        for (TicketTransitionEntity entity : batch) {
                            batchBuilder.addPutItem(entity);
                        }
                        BatchWriteItemEnhancedRequest request = BatchWriteItemEnhancedRequest.builder()
                                .addWriteBatch(batchBuilder.build())
                                .build();
                        enhancedClient.batchWriteItem(request);
                    }
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Flux<TicketTransition> findByTicketId(TicketId ticketId) {
        return Mono.fromCallable(() -> {
                    DynamoDbIndex<TicketTransitionEntity> index =
                            transitionTable.index("ticket-id-index");
                    QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                            .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                                    .partitionValue(ticketId.value().toString())
                                    .build()))
                            .build();

                    return index.query(queryRequest)
                            .stream()
                            .flatMap(page -> page.items().stream())
                            .map(TicketTransitionPersistenceMapper::toDomain)
                            .toList();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }
}
