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
public class BillingEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(BillingEventConsumer.class);
    private final AuditLogRepository repository; private final ObjectMapper objectMapper;
    public BillingEventConsumer(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository; this.objectMapper = objectMapper;
    }
    @KafkaListener(topics = "${app.kafka.billing-events-topic:billing-events}",
            groupId = "${spring.kafka.consumer.group-id:audit-service}")
    public void consume(ConsumerRecord<String, byte[]> record) throws IOException {
        JsonNode event = objectMapper.readTree(record.value());
        String type = event.path("eventType").asText("BillingEvent");
        String actor = event.path("actorId").asText("system");
        LocalDateTime timestamp = event.hasNonNull("occurredAt")
                ? OffsetDateTime.parse(event.get("occurredAt").asText()).toLocalDateTime() : LocalDateTime.now();
        repository.save(new AuditLog(type, new String(record.value(), StandardCharsets.UTF_8), actor, timestamp));
        logger.info("Saved audit event {} for billing account {}", type, event.path("billingAccountId").asText());
    }
}
