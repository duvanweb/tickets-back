package com.tickets.infrastructure.adapter.out.sqs;

import com.tickets.infrastructure.config.AwsSqsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsProducerTest {

    @Mock
    private SqsClient sqsClient;

    private SqsProducer producer;

    @BeforeEach
    void setUp() {
        AwsSqsProperties props = new AwsSqsProperties(
                "us-east-1", "http://localhost:4566", "order-created-queue");
        producer = new SqsProducer(sqsClient, props);
    }

    @Test
    void publish_resolvesQueueUrlAndSendsMessage() {
        String queueUrl = "http://localhost:4566/000000000000/order-created-queue";
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-1").build());

        StepVerifier.create(producer.publish("{\"event\":\"created\"}"))
                .verifyComplete();

        verify(sqsClient).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void publish_cachesQueueUrlAndDoesNotResolveTwice() {
        String queueUrl = "http://localhost:4566/000000000000/order-created-queue";
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-1").build());

        StepVerifier.create(producer.publish("first"))
                .verifyComplete();
        StepVerifier.create(producer.publish("second"))
                .verifyComplete();

        verify(sqsClient, times(1)).getQueueUrl(any(GetQueueUrlRequest.class));
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }
}
