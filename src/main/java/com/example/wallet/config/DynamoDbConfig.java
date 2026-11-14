package com.example.wallet.config;

import com.example.wallet.dynamodb.TransferReadModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(
            @Value("${wallet.dynamodb.region:eu-west-2}") String region,
            @Value("${wallet.dynamodb.endpoint:http://localhost:8000}") String endpoint) {

        var builder = DynamoDbClient.builder()
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(
                                            "dummy",
                                            "dummy"
                                    )
                            )
                    );
        } else {
            builder.credentialsProvider(
                    DefaultCredentialsProvider.create()
            );
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(
            DynamoDbClient client) {

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public DynamoDbTable<TransferReadModel> transferReadModelTable(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${wallet.dynamodb.table-name:wallet-transfer-read-model}")
            String tableName) {

        return enhancedClient.table(
                tableName,
                TableSchema.fromBean(TransferReadModel.class)
        );
    }
}