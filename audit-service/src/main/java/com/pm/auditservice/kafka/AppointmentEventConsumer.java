package com.pm.auditservice.kafka;

import com.fasterxml.jackson.databind.*;
import com.pm.auditservice.model.AuditLog;
import com.pm.auditservice.repository.AuditLogRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.*;

@Component
public class AppointmentEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentEventConsumer.class);
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AppointmentEventConsumer(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository; this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.appointment-events-topic:appointment-events}",
            groupId = "${spring.kafka.consumer.group-id:audit-service}")
    public void consume(ConsumerRecord<String, byte[]> record) throws IOException {
        JsonNode event = objectMapper.readTree(record.value());
        String eventType = event.path("eventType").asText("AppointmentEvent");
        String actorId = event.path("actorId").asText("system");
        LocalDateTime timestamp = event.hasNonNull("occurredAt")
                ? OffsetDateTime.parse(event.get("occurredAt").asText()).toLocalDateTime()
                : LocalDateTime.now();
        repository.save(new AuditLog(eventType,
                new String(record.value(), StandardCharsets.UTF_8), actorId, timestamp));
        logger.info("Saved audit event {} for appointment {}", eventType, event.path("appointmentId").asLong());
    }
}
