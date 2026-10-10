package com.tickets.infrastructure.adapter.out.sqs;

import com.tickets.application.port.out.publishers.MessagePublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adapter that implements the {@link MessagePublisher} port by delegating
 * to {@link SqsProducer}.
 */
@Component
public class SqsMessagePublisherAdapter implements MessagePublisher {

    private final SqsProducer sqsProducer;

    public SqsMessagePublisherAdapter(SqsProducer sqsProducer) {
        this.sqsProducer = sqsProducer;
    }

    @Override
    public Mono<Void> publish(String messageBody) {
        return sqsProducer.publish(messageBody);
    }
}
