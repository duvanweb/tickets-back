package com.tickets.infrastructure.config;

import com.tickets.infrastructure.adapter.out.persistence.entity.EventEntity;
import com.tickets.infrastructure.adapter.out.persistence.entity.OrderEntity;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.tickets.infrastructure.adapter.out.persistence.entity.TicketTransitionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

/**
 * Spring configuration for DynamoDB beans.
 *
 * <p>Creates the {@link DynamoDbClient}, {@link DynamoDbEnhancedClient},
 * and {@link DynamoDbTable} for events. A {@link CommandLineRunner} ensures
 * the events table exists at startup (for local development).</p>
 */
@Configuration
@EnableConfigurationProperties(AwsDynamoDbProperties.class)
public class DynamoDbConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfig.class);

    @Bean
    public DynamoDbClient dynamoDbClient(AwsDynamoDbProperties props) {
        var builder = DynamoDbClient.builder()
                .region(Region.of(props.region()));
        if (props.endpoint() != null && !props.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.endpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public DynamoDbTable<EventEntity> eventTable(DynamoDbEnhancedClient enhancedClient,
                                                  AwsDynamoDbProperties props) {
        return enhancedClient.table(props.eventsTableName(), TableSchema.fromBean(EventEntity.class));
    }

    @Bean
    public DynamoDbTable<TicketEntity> ticketTable(DynamoDbEnhancedClient enhancedClient,
                                                   AwsDynamoDbProperties props) {
        return enhancedClient.table(props.tableName(), TableSchema.fromBean(TicketEntity.class));
    }

    @Bean
    @Profile("!worker")
    public CommandLineRunner createTicketTable(DynamoDbClient client, AwsDynamoDbProperties props) {
        return args -> {
            try {
                client.createTable(CreateTableRequest.builder()
                        .tableName(props.tableName())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("ticketId")
                                .keyType(KeyType.HASH)
                                .build())
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName("ticketId")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("eventId")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("status")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("reservedUntil")
                                        .attributeType(ScalarAttributeType.S)
                                        .build())
                        .globalSecondaryIndexes(
                                GlobalSecondaryIndex.builder()
                                        .indexName("event-status-index")
                                        .keySchema(
                                                KeySchemaElement.builder()
                                                        .attributeName("eventId")
                                                        .keyType(KeyType.HASH)
                                                        .build(),
                                                KeySchemaElement.builder()
                                                        .attributeName("status")
                                                        .keyType(KeyType.RANGE)
                                                        .build())
                                        .projection(Projection.builder()
                                                .projectionType(ProjectionType.ALL)
                                                .build())
                                        .build(),
                                GlobalSecondaryIndex.builder()
                                        .indexName("status-reservedUntil-index")
                                        .keySchema(
                                                KeySchemaElement.builder()
                                                        .attributeName("status")
                                                        .keyType(KeyType.HASH)
                                                        .build(),
                                                KeySchemaElement.builder()
                                                        .attributeName("reservedUntil")
                                                        .keyType(KeyType.RANGE)
                                                        .build())
                                        .projection(Projection.builder()
                                                .projectionType(ProjectionType.ALL)
                                                .build())
                                        .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());
                log.info("DynamoDB table '{}' created", props.tableName());
            } catch (ResourceInUseException ignored) {
                log.debug("DynamoDB table '{}' already exists", props.tableName());
            } catch (Exception e) {
                log.warn("Failed to create DynamoDB table '{}': {}", props.tableName(), e.getMessage());
            }
        };
    }

    @Bean
    public DynamoDbTable<OrderEntity> orderTable(DynamoDbEnhancedClient enhancedClient,
                                                  AwsDynamoDbProperties props) {
        return enhancedClient.table(props.ordersTableName(), TableSchema.fromBean(OrderEntity.class));
    }

    @Bean
    @Profile("!worker")
    public CommandLineRunner createOrderTable(DynamoDbClient client, AwsDynamoDbProperties props) {
        return args -> {
            try {
                client.createTable(CreateTableRequest.builder()
                        .tableName(props.ordersTableName())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("orderId")
                                .keyType(KeyType.HASH)
                                .build())
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName("orderId")
                                        .attributeType(ScalarAttributeType.S)
                                        .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());
                log.info("DynamoDB table '{}' created", props.ordersTableName());
            } catch (ResourceInUseException ignored) {
                log.debug("DynamoDB table '{}' already exists", props.ordersTableName());
            } catch (Exception e) {
                log.warn("Failed to create DynamoDB table '{}': {}", props.ordersTableName(), e.getMessage());
            }
        };
    }

    @Bean
    @Profile("!worker")
    public CommandLineRunner createEventTable(DynamoDbClient client, AwsDynamoDbProperties props) {
        return args -> {
            try {
                client.createTable(CreateTableRequest.builder()
                        .tableName(props.eventsTableName())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("eventId")
                                .keyType(KeyType.HASH)
                                .build())
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName("eventId")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("name")
                                        .attributeType(ScalarAttributeType.S)
                                        .build())
                        .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                                .indexName("name-index")
                                .keySchema(KeySchemaElement.builder()
                                        .attributeName("name")
                                        .keyType(KeyType.HASH)
                                        .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());
                log.info("DynamoDB table '{}' created", props.eventsTableName());
            } catch (ResourceInUseException ignored) {
                log.debug("DynamoDB table '{}' already exists", props.eventsTableName());
            } catch (Exception e) {
                log.warn("Failed to create DynamoDB table '{}': {}", props.eventsTableName(), e.getMessage());
            }
        };
    }

    @Bean
    public DynamoDbTable<TicketTransitionEntity> transitionTable(DynamoDbEnhancedClient enhancedClient,
                                                                  AwsDynamoDbProperties props) {
        return enhancedClient.table(props.transitionsTableName(),
                TableSchema.fromBean(TicketTransitionEntity.class));
    }

    @Bean
    @Profile("!worker")
    public CommandLineRunner createTransitionTable(DynamoDbClient client, AwsDynamoDbProperties props) {
        return args -> {
            try {
                client.createTable(CreateTableRequest.builder()
                        .tableName(props.transitionsTableName())
                        .keySchema(KeySchemaElement.builder()
                                .attributeName("transitionId")
                                .keyType(KeyType.HASH)
                                .build())
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName("transitionId")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("ticketId")
                                        .attributeType(ScalarAttributeType.S)
                                        .build(),
                                AttributeDefinition.builder()
                                        .attributeName("timestamp")
                                        .attributeType(ScalarAttributeType.S)
                                        .build())
                        .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                                .indexName("ticket-id-index")
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("ticketId")
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName("timestamp")
                                                .keyType(KeyType.RANGE)
                                                .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());
                log.info("DynamoDB table '{}' created", props.transitionsTableName());
            } catch (ResourceInUseException ignored) {
                log.debug("DynamoDB table '{}' already exists", props.transitionsTableName());
            } catch (Exception e) {
                log.warn("Failed to create DynamoDB table '{}': {}", props.transitionsTableName(), e.getMessage());
            }
        };
    }
}
