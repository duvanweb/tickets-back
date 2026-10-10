package com.tickets.infrastructure.adapter.out.sqs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsMessagePublisherAdapterTest {

    @Mock
    private SqsProducer sqsProducer;

    @InjectMocks
    private SqsMessagePublisherAdapter adapter;

    @Test
    void publish_delegatesToSqsProducer() {
        String message = "{\"event\":\"created\"}";
        when(sqsProducer.publish(message)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.publish(message))
                .verifyComplete();

        verify(sqsProducer).publish(message);
    }
}
