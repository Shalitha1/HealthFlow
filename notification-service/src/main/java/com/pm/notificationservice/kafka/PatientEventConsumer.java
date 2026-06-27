package com.pm.notificationservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.event.PatientCreatedEvent;
import com.pm.notificationservice.service.EmailNotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PatientEventConsumer {

    private final EmailNotificationService emailNotificationService;

    public PatientEventConsumer(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @KafkaListener(
            topics = "${app.kafka.patient-events-topic:patient-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void consume(ConsumerRecord<String, byte[]> record) throws InvalidProtocolBufferException {
        PatientCreatedEvent event = PatientCreatedEvent.parseFrom(record.value());

        emailNotificationService.sendWelcomeEmail(event);
    }
}