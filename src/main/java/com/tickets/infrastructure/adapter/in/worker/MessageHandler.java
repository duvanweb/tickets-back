package com.tickets.infrastructure.adapter.in.worker;

import reactor.core.publisher.Mono;

/**
 * Interface for handling SQS messages by type.
 *
 * <p>Each implementation handles a specific {@code messageType} (e.g.
 * {@code "order-created"}). The {@link SqsMessageConsumer} routes
 * incoming messages to the matching handler based on the
 * {@code type} field in the message body.</p>
 */
public interface MessageHandler {

    /**
     * Returns the message type this handler processes.
     *
     * @return the message type string
     */
    String messageType();

    /**
     * Handles the message payload.
     *
     * @param payload the JSON payload string from the SQS message
     * @return a {@link Mono} that completes when processing is done,
     *         or errors if processing fails
     */
    Mono<Void> handle(String payload);
}
