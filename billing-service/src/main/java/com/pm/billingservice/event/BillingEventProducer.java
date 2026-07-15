package com.pm.billingservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.billingservice.model.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class BillingEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(BillingEventProducer.class);
    private final KafkaTemplate<String, byte[]> template; private final ObjectMapper mapper; private final String topic;
    public BillingEventProducer(KafkaTemplate<String, byte[]> template, ObjectMapper mapper,
            @Value("${app.kafka.billing-events-topic:billing-events}") String topic) {
        this.template = template; this.mapper = mapper; this.topic = topic;
    }
    public void publish(String type, BillingAccount account, Invoice invoice, Payment payment, BigDecimal amount, String actorId) {
        BillingEvent event = new BillingEvent(type, account.getId(), invoice == null ? null : invoice.getId(),
                payment == null ? null : payment.getId(), account.getPatientId(), amount,
                invoice == null ? account.getStatus().name() : invoice.getStatus().name(),
                actorId == null || actorId.isBlank() ? "system" : actorId, OffsetDateTime.now());
        try {
            template.send(topic, account.getId(), mapper.writeValueAsBytes(event));
            logger.info("Published {} for billing account {}", type, account.getId());
        } catch (JsonProcessingException exception) {
            logger.error("Could not serialize billing event", exception);
        }
    }
}
