package com.tickets.infrastructure.adapter.in.worker;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tickets.domain.exception.DomainException;
import com.tickets.infrastructure.config.AwsSqsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SQS message consumer that polls the queue on a fixed schedule.
 *
 * <p>Parses each message body as JSON with a {@code type} and
 * {@code payload} field, routes to the matching {@link MessageHandler},
 * and deletes the message from the queue on success. On failure the
 * message is not deleted and returns to the queue after the visibility
 * timeout.</p>
 */
@Component
@Profile("worker")
public class SqsMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageConsumer.class);

    private final SqsClient sqsClient;
    private final AwsSqsProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, MessageHandler> handlers;

    public SqsMessageConsumer(SqsClient sqsClient,
                              AwsSqsProperties properties,
                              ObjectMapper objectMapper,
                              java.util.List<MessageHandler> handlerList) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(MessageHandler::messageType, Function.identity()));
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms:1000}")
    public void pollAndProcess() {
        var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .maxNumberOfMessages(properties.maxNumberOfMessages())
                .waitTimeSeconds(properties.waitTimeSeconds())
                .build());

        for (Message message : response.messages()) {
            processMessage(message);
        }
    }

    private void processMessage(Message message) {
        try {
            JsonNode json = objectMapper.readTree(message.body());
            String type = json.get("type").asText();
            String payload = json.get("payload").toString();

            MessageHandler handler = handlers.get(type);
            if (handler == null) {
                log.warn("No handler registered for message type '{}', skipping", type);
                return;
            }

            handler.handle(payload).block();
            deleteMessage(message);
            log.debug("Processed and deleted SQS message id={}, type={}", message.messageId(), type);

        } catch (Exception e) {
            if (isPermanentFailure(e)) {
                log.error("Permanent failure for SQS message id={}, deleting from queue: {}",
                        message.messageId(), e.getMessage(), e);
                deleteMessage(message);
            } else {
                log.error("Failed to process SQS message id={}, leaving in queue: {}",
                        message.messageId(), e.getMessage(), e);
            }
        }
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .receiptHandle(message.receiptHandle())
                .build());
    }

    /**
     * Determines if the failure is permanent (retrying will never succeed).
     *
     * <p>Domain exceptions ({@link DomainException}) represent permanent
     * business rule violations — e.g. invalid order, order not found, invalid
     * event. These should be deleted from the queue to avoid poison messages.
     * Other exceptions (e.g. transient infrastructure errors) are left in
     * the queue for retry.</p>
     */
    private boolean isPermanentFailure(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof DomainException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
