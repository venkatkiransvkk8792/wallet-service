package com.example.wallet.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TransferReadModelRepository {
    private final DynamoDbTable<TransferReadModel> table;

    public TransferReadModelRepository(DynamoDbEnhancedClient client,
                                       @Value("${wallet.dynamodb.table-name:wallet-transfer-read-model}") String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(TransferReadModel.class));
    }

    public void save(TransferReadModel item) {
        table.putItem(item);
    }

    public List<TransferReadModel> findByWalletId(String walletId, int limit) {
        var request = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(walletId).build());
        var result = table.query(r -> r.queryConditional(request).limit(limit));
        List<TransferReadModel> items = new ArrayList<>();
        result.stream().flatMap(page -> page.items().stream()).limit(limit).forEach(items::add);
        return items;
    }
}
