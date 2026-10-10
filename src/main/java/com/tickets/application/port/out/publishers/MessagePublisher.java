package com.tickets.application.port.out.publishers;

import reactor.core.publisher.Mono;

/**
 * Secondary port (publisher interface) for sending messages.
 *
 * <p>Generic out-port decoupled from any specific domain type — the
 * message body is a raw {@link String}. Implementations are responsible
 * for delivering the message to the underlying messaging infrastructure.</p>
 */
public interface MessagePublisher {

    /**
     * Publishes the given message body.
     *
     * @param messageBody the raw message body to send
     * @return a {@link Mono} that completes when the message has been sent
     */
    Mono<Void> publish(String messageBody);
}
