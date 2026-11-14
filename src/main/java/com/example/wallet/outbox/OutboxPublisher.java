package com.example.wallet.outbox;

import com.example.wallet.domain.OutboxEvent;
import com.example.wallet.domain.OutboxStatus;
import com.example.wallet.kafka.TransferCompletedEvent;
import com.example.wallet.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "wallet.kafka.enabled", havingValue = "true")
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository repository,
                           KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${wallet.outbox.poll-ms:1000}")
    public void publish() {
        var events = repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);
        for (OutboxEvent event : events) {
            try {
                var payload = objectMapper.readValue(event.getPayload(), TransferCompletedEvent.class);
                // transferId is the Kafka key, preserving ordering for events for one transfer.
                kafkaTemplate.send(event.getTopic(), payload.transferId().toString(), payload)
                        .whenComplete((result, error) -> {
                            if (error == null) {
                                markPublished(event.getEventId());
                            } else {
                                log.warn("Kafka publish failed for outbox event {}", event.getEventId(), error);
                            }
                        });
            } catch (Exception e) {
                log.warn("Unable to publish outbox event {}; it will be retried", event.getEventId(), e);
            }
        }
    }

    private void markPublished(java.util.UUID eventId) {
        repository.findById(eventId).ifPresent(event -> {
            if (event.getStatus() == OutboxStatus.NEW) {
                event.markPublished();
                repository.save(event);
            }
        });
    }
}
