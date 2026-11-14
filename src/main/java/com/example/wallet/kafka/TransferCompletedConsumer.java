package com.example.wallet.kafka;

import com.example.wallet.dynamodb.TransferReadModel;
import com.example.wallet.dynamodb.TransferReadModelRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "wallet.kafka.enabled", havingValue = "true")
public class TransferCompletedConsumer {
    private final TransferReadModelRepository repository;

    public TransferCompletedConsumer(TransferReadModelRepository repository) {
        this.repository = repository;
    }

    @RetryableTopic(
            attempts = "3",
            //numPartitions = "3",
            //replicationFactor = "1",
            //we need to set the partition mapping strategy to use the same partition for retries, otherwise we lose ordering guarantees
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0
            )
    )
    @KafkaListener(
            topics = KafkaTopics.TRANSFER_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id}"
            //,containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TransferCompletedEvent event) {
        repository.save(toReadModel(event, event.fromWalletId(), event.toWalletId(), "DEBIT"));
        repository.save(toReadModel(event, event.toWalletId(), event.fromWalletId(), "CREDIT"));
    }

    private TransferReadModel toReadModel(TransferCompletedEvent event,
                                          String walletId,
                                          String counterparty,
                                          String direction) {
        var item = new TransferReadModel();
        item.setWalletId(walletId);
        item.setSortKey(event.occurredAt() + "#" + event.transferId() + "#" + direction);
        item.setTransferId(event.transferId().toString());
        item.setDirection(direction);
        item.setCounterpartyWalletId(counterparty);
        item.setAmount(event.amount());
        item.setOccurredAt(event.occurredAt().toString());
        item.setIdempotencyKey(event.idempotencyKey());
        return item;
    }
}
