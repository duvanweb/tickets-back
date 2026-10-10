package com.tickets.infrastructure.adapter.out.sqs;

import com.tickets.infrastructure.config.AwsSqsProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Low-level SQS operations for publishing messages.
 *
 * <p>The blocking {@code sendMessage} call is wrapped in
 * {@code Mono.fromRunnable} and subscribed on
 * {@code Schedulers.boundedElastic()} to avoid blocking the Netty event
 * loop. The queue URL is resolved once via {@code getQueueUrl} and cached
 * for subsequent calls.</p>
 */
@Component
public class SqsProducer {

    private final SqsClient sqsClient;
    private final AwsSqsProperties props;
    private final AtomicReference<String> cachedQueueUrl = new AtomicReference<>();

    public SqsProducer(SqsClient sqsClient, AwsSqsProperties props) {
        this.sqsClient = sqsClient;
        this.props = props;
    }

    /**
     * Sends the given message body to the configured SQS queue.
     *
     * @param messageBody the raw message body
     * @return a {@link Mono} that completes when the message has been sent
     */
    public Mono<Void> publish(String messageBody) {
        return Mono.fromRunnable(() -> doSend(messageBody))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void doSend(String messageBody) {
        String queueUrl = resolveQueueUrl();
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build());
    }

    private String resolveQueueUrl() {
        String url = cachedQueueUrl.get();
        if (url == null) {
            url = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(props.queueName())
                    .build()).queueUrl();
            cachedQueueUrl.set(url);
        }
        return url;
    }
}
