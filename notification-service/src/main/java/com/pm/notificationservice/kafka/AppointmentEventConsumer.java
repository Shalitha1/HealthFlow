package com.pm.notificationservice.kafka;

import com.fasterxml.jackson.databind.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AppointmentEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentEventConsumer.class);
    private final ObjectMapper objectMapper;

    public AppointmentEventConsumer(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @KafkaListener(topics = "${app.kafka.appointment-events-topic:appointment-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void consume(ConsumerRecord<String, byte[]> record) throws IOException {
        JsonNode event = objectMapper.readTree(record.value());
        logger.info("Would notify patient {} about {} for appointment {}",
                event.path("patientId").asLong(), event.path("eventType").asText(),
                event.path("appointmentId").asLong());
    }
}
