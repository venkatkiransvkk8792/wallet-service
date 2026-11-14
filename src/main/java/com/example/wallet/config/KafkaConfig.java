package com.example.wallet.config;

import com.example.wallet.kafka.KafkaTopics;
import com.example.wallet.kafka.TransferCompletedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "wallet.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, TransferCompletedEvent> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        var props = new java.util.HashMap<String, Object>();
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate(
            ProducerFactory<String, TransferCompletedEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, TransferCompletedEvent> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        var props = new java.util.HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        var deserializer = new JsonDeserializer<>(TransferCompletedEvent.class);
        deserializer.addTrustedPackages("com.example.wallet.kafka");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /*@Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransferCompletedEvent>
    kafkaListenerContainerFactory(ConsumerFactory<String, TransferCompletedEvent> consumerFactory,
                                  KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TransferCompletedEvent>();
        factory.setConsumerFactory(consumerFactory);
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".DLT", record.partition()));
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L)));
        return factory;
    }*/

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransferCompletedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, TransferCompletedEvent> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TransferCompletedEvent>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public NewTopic transferCompletedTopic(
            @Value("${wallet.kafka.partitions:3}") int partitions,
            @Value("${wallet.kafka.replicas:1}") short replicas) {
        return new NewTopic(KafkaTopics.TRANSFER_COMPLETED, partitions, replicas);
    }

    /*@Bean
    @ConditionalOnProperty(
            name = "wallet.kafka.dlt.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public NewTopic transferCompletedDltTopic(
            @Value("${wallet.kafka.partitions:3}") int partitions,
            @Value("${wallet.kafka.replicas:1}") short replicas) {

        return new NewTopic(
                KafkaTopics.TRANSFER_COMPLETED + ".DLT",
                partitions,
                replicas
        );
    }*/
}
