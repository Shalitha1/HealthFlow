package com.pm.appointmentservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.appointmentservice.model.Appointment;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
public class AppointmentEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentEventProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public AppointmentEventProducer(KafkaTemplate<String, byte[]> kafkaTemplate, ObjectMapper objectMapper,
            @Value("${app.kafka.appointment-events-topic:appointment-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate; this.objectMapper = objectMapper; this.topic = topic;
    }

    public void publish(String eventType, Appointment appointment, String actorId) {
        try {
            AppointmentEvent event = new AppointmentEvent(eventType, appointment.getId(),
                    appointment.getPatientId(), appointment.getDoctorId(), appointment.getAppointmentDateTime(),
                    appointment.getDurationMinutes(), appointment.getStatus().name(), actorId, OffsetDateTime.now());
            kafkaTemplate.send(topic, appointment.getId().toString(), objectMapper.writeValueAsBytes(event));
            logger.info("Published {} for appointment {}", eventType, appointment.getId());
        } catch (JsonProcessingException exception) {
            logger.error("Could not serialize appointment event", exception);
        }
    }
}
