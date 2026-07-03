package com.pm.patientmanagement.kafka;

import com.pm.event.PatientCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka event producer - publishes patient events
 */
@Component
public class PatientEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(PatientEventProducer.class);
    private static final String PATIENT_EVENTS_TOPIC = "patient-events";

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    /**
     * Publish patient created event
     */
    public void publishPatientCreatedEvent(Long patientId, String name, String email, String address, String dateOfBirth) {
        try {
            PatientCreatedEvent event = PatientCreatedEvent.newBuilder()
                    .setPatientId(patientId)
                    .setName(name)
                    .setEmail(email)
                    .setAddress(address)
                    .setDateOfBirth(dateOfBirth)
                    .setCreatedAt(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(PATIENT_EVENTS_TOPIC, patientId.toString(), event.toByteArray());
            logger.info("Published patient created event: {}", patientId);

        } catch (Exception e) {
            logger.error("Error publishing patient created event", e);
        }
    }
}
