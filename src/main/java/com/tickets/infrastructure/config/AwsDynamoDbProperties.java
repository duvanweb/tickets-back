package com.tickets.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AWS DynamoDB.
 *
 * @param region          the AWS region
 * @param endpoint        the DynamoDB endpoint override (for local development; null in production)
 * @param tableName       the name of the tickets table
 * @param eventsTableName the name of the events table
 */
@ConfigurationProperties(prefix = "aws.dynamodb")
public record AwsDynamoDbProperties(
        String region,
        String endpoint,
        String tableName,
        String eventsTableName) {
}
