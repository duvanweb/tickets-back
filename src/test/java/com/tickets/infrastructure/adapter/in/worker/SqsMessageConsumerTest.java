package com.tickets.infrastructure.adapter.in.worker;

import tools.jackson.databind.json.JsonMapper;
import com.tickets.infrastructure.config.AwsSqsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsMessageConsumerTest {

    private static final AwsSqsProperties PROPS = new AwsSqsProperties(
            "us-east-1", "http://localhost:4566",
            "order-created-queue",
            "http://localhost:4566/000000000000/order-created-queue",
            10, 20, 1000);

    @Mock
    private SqsClient sqsClient;

    @Mock
    private MessageHandler orderCreatedHandler;

    private SqsMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        when(orderCreatedHandler.messageType()).thenReturn("order-created");
        consumer = new SqsMessageConsumer(
                sqsClient, PROPS, JsonMapper.builder().build(), List.of(orderCreatedHandler));
    }

    @Test
    void pollAndProcess_withValidMessage_routesToHandlerAndDeletes() {
        Message message = Message.builder()
                .messageId("msg-1")
                .receiptHandle("receipt-1")
                .body("""
                        {"type":"order-created","payload":{"orderId":"ord-123","eventId":"evt-456","quantity":2}}""")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(orderCreatedHandler.handle(any(String.class))).thenReturn(Mono.empty());
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        consumer.pollAndProcess();

        verify(orderCreatedHandler).handle(any(String.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollAndProcess_whenHandlerThrows_doesNotDeleteMessage() {
        Message message = Message.builder()
                .messageId("msg-2")
                .receiptHandle("receipt-2")
                .body("""
                        {"type":"order-created","payload":{"orderId":"ord-123","eventId":"evt-456","quantity":2}}""")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(orderCreatedHandler.handle(any(String.class)))
                .thenReturn(Mono.error(new RuntimeException("Processing failed")));

        consumer.pollAndProcess();

        verify(orderCreatedHandler).handle(any(String.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollAndProcess_withUnknownType_doesNotDeleteAndDoesNotCallHandler() {
        Message message = Message.builder()
                .messageId("msg-3")
                .receiptHandle("receipt-3")
                .body("""
                        {"type":"unknown-type","payload":{}}""")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());

        consumer.pollAndProcess();

        verify(orderCreatedHandler, never()).handle(any(String.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollAndProcess_withNoMessages_doesNothing() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().build());

        consumer.pollAndProcess();

        verify(orderCreatedHandler, never()).handle(any(String.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollAndProcess_withMultipleMessages_processesAll() {
        Message msg1 = Message.builder()
                .messageId("msg-a")
                .receiptHandle("receipt-a")
                .body("""
                        {"type":"order-created","payload":{"orderId":"ord-1","eventId":"evt-1","quantity":1}}""")
                .build();
        Message msg2 = Message.builder()
                .messageId("msg-b")
                .receiptHandle("receipt-b")
                .body("""
                        {"type":"order-created","payload":{"orderId":"ord-2","eventId":"evt-2","quantity":3}}""")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(msg1, msg2).build());
        when(orderCreatedHandler.handle(any(String.class))).thenReturn(Mono.empty());
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        consumer.pollAndProcess();

        verify(orderCreatedHandler, times(2)).handle(any(String.class));
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }
}
