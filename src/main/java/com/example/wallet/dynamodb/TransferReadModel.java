package com.example.wallet.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;

@DynamoDbBean
public class TransferReadModel {
    private String walletId;
    private String sortKey;
    private String transferId;
    private String direction;
    private String counterpartyWalletId;
    private BigDecimal amount;
    private String occurredAt;
    private String idempotencyKey;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("walletId")
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    @DynamoDbSortKey
    @DynamoDbAttribute("sortKey")
    public String getSortKey() { return sortKey; }
    public void setSortKey(String sortKey) { this.sortKey = sortKey; }

    public String getTransferId() { return transferId; }
    public void setTransferId(String transferId) { this.transferId = transferId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getCounterpartyWalletId() { return counterpartyWalletId; }
    public void setCounterpartyWalletId(String counterpartyWalletId) { this.counterpartyWalletId = counterpartyWalletId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
