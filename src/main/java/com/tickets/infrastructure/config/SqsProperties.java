package com.tickets.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AWS SQS.
 *
 * @param region              the AWS region
 * @param endpoint            the SQS endpoint override (for LocalStack; null in production)
 * @param queueUrl            the URL of the SQS queue to consume from
 * @param maxNumberOfMessages the maximum number of messages to retrieve per poll (1-10)
 * @param waitTimeSeconds     the long polling wait time in seconds (0-20)
 * @param pollIntervalMs      the delay in milliseconds between poll cycles
 */
@ConfigurationProperties(prefix = "aws.sqs")
public record SqsProperties(
        String region,
        String endpoint,
        String queueUrl,
        int maxNumberOfMessages,
        int waitTimeSeconds,
        int pollIntervalMs) {
}
