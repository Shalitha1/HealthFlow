package com.pm.patientmanagement.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import com.pm.event.PatientCreatedEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * ProducerFactory: Configures how Kafka producers serialize messages
     * - Key serializer: StringSerializer (topic name or message ID as String)
     * - Value serializer: JsonSerializer (PatientCreatedEvent → JSON)
     */
    @Bean
    public ProducerFactory<String, PatientCreatedEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Where to find Kafka brokers
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // How to serialize the message key (String)
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // How to serialize the message value (PatientCreatedEvent → JSON)
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Send acknowledgment when message is written to at least 1 replica
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");

        // Retry up to 3 times on failure
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate: Spring wrapper around Kafka producer
     * Use this to send messages: kafkaTemplate.send("topic-name", event)
     */
    @Bean
    public KafkaTemplate<String, PatientCreatedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}