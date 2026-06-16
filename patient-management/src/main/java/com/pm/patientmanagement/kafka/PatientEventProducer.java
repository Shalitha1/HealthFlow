package com.pm.patientmanagement.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka event producer - publishes patient events
 */
@Component
public class PatientEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(PatientEventProducer.class);
    private static final String PATIENT_EVENTS_TOPIC = "patient-events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Publish patient created event
     */
    public void publishPatientCreatedEvent(Long patientId, String name, String email, String address, String dateOfBirth) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("patientId", patientId);
            event.put("name", name);
            event.put("email", email);
            event.put("address", address);
            event.put("dateOfBirth", dateOfBirth);
            event.put("createdAt", System.currentTimeMillis());

            String payload = objectMapper.writeValueAsString(event);

            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, PATIENT_EVENTS_TOPIC)
                    .setHeader("event_type", "PATIENT_CREATED")
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(message);
            logger.info("✅ Published patient event: {}", patientId);

        } catch (Exception e) {
            logger.error("❌ Error publishing patient event", e);
        }
    }
}