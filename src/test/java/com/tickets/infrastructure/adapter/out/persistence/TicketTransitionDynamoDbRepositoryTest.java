package com.tickets.infrastructure.adapter.out.persistence;

import com.tickets.domain.model.ticket.TicketId;
import com.tickets.domain.model.ticket.TicketStatus;
import com.tickets.domain.model.ticket.TicketTransition;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketTransitionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketTransitionDynamoDbRepositoryTest {

    @Mock
    private DynamoDbTable<TicketTransitionEntity> transitionTable;

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @InjectMocks
    private TicketTransitionDynamoDbRepository repository;

    @Test
    void saveAll_withTransitions_callsBatchWriteItem() {
        TicketId ticketId = TicketId.generate();
        List<TicketTransition> transitions = List.of(
                TicketTransition.create(ticketId, TicketStatus.AVAILABLE, TicketStatus.RESERVED, "order-1"),
                TicketTransition.create(ticketId, TicketStatus.RESERVED, TicketStatus.SOLD, "order-1"));

        when(transitionTable.tableSchema()).thenReturn(TableSchema.fromBean(TicketTransitionEntity.class));

        StepVerifier.create(repository.saveAll(transitions))
                .verifyComplete();

        verify(enhancedClient, times(1)).batchWriteItem(any(BatchWriteItemEnhancedRequest.class));
    }

    @Test
    void saveAll_withEmptyList_returnsEmpty() {
        StepVerifier.create(repository.saveAll(List.of()))
                .verifyComplete();

        verifyNoInteractions(enhancedClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByTicketId_returnsTransitionsForTicket() {
        TicketId ticketId = TicketId.generate();
        TicketTransitionEntity entity = new TicketTransitionEntity();
        entity.setTransitionId(java.util.UUID.randomUUID().toString());
        entity.setTicketId(ticketId.value().toString());
        entity.setFromStatus("AVAILABLE");
        entity.setToStatus("RESERVED");
        entity.setOrderId("order-123");
        entity.setTimestamp(java.time.LocalDateTime.now().toString());

        DynamoDbIndex<TicketTransitionEntity> index = mock(DynamoDbIndex.class);
        SdkIterable<Page<TicketTransitionEntity>> sdkIterable = mock(SdkIterable.class);
        Page<TicketTransitionEntity> page = mock(Page.class);

        when(transitionTable.index("ticket-id-index")).thenReturn(index);
        when(index.query(any(QueryEnhancedRequest.class))).thenReturn(sdkIterable);
        when(sdkIterable.stream()).thenReturn(Stream.of(page));
        when(page.items()).thenReturn(List.of(entity));

        StepVerifier.create(repository.findByTicketId(ticketId))
                .assertNext(transition -> {
                    assertThat(transition.ticketId()).isEqualTo(ticketId);
                    assertThat(transition.fromStatus()).isEqualTo(TicketStatus.AVAILABLE);
                    assertThat(transition.toStatus()).isEqualTo(TicketStatus.RESERVED);
                })
                .verifyComplete();
    }
}
