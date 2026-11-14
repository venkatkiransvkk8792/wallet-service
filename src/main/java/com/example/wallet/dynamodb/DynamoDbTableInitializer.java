package com.example.wallet.dynamodb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@Configuration
public class DynamoDbTableInitializer {

    @Bean
    CommandLineRunner createReadModelTable(
            DynamoDbEnhancedClient client,
            @Value("${wallet.dynamodb.table-name:wallet-transfer-read-model}")
            String tableName,
            @Value("${wallet.dynamodb.auto-create-table:true}")
            boolean autoCreate) {

        return args -> {

            System.out.println("========================================");
            System.out.println("DynamoDB TABLE INITIALIZATION");
            System.out.println("tableName  = " + tableName);
            System.out.println("autoCreate = " + autoCreate);
            System.out.println("========================================");

            if (!autoCreate) {
                System.out.println(
                        "DynamoDB auto-create is DISABLED"
                );
                return;
            }

            var table = client.table(
                    tableName,
                    TableSchema.fromBean(TransferReadModel.class)
            );

            try {

                System.out.println(
                        "Checking table: " + tableName
                );

                table.describeTable();

                System.out.println(
                        "Table already exists: " + tableName
                );

            } catch (ResourceNotFoundException e) {

                System.out.println(
                        "Table does not exist. Creating: " + tableName
                );

                table.createTable(
                        CreateTableEnhancedRequest.builder()
                                .provisionedThroughput(
                                        b -> b
                                                .readCapacityUnits(5L)
                                                .writeCapacityUnits(5L)
                                )
                                .build()
                );

                System.out.println(
                        "TABLE CREATION REQUEST SENT: " + tableName
                );
            }
        };
    }
}