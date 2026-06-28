package com.pm.auditservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pm.auditservice.model.AuditLog;
import com.pm.auditservice.repository.AuditLogRepository;
import com.pm.event.PatientCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PatientEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PatientEventConsumer.class);

    private final AuditLogRepository auditLogRepository;

    public PatientEventConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @KafkaListener(
            topics = "${app.kafka.patient-events-topic:patient-events}",
            groupId = "${spring.kafka.consumer.group-id:audit-service}"
    )
    public void consume(ConsumerRecord<String, byte[]> record) throws InvalidProtocolBufferException {
        PatientCreatedEvent event = PatientCreatedEvent.parseFrom(record.value());

        AuditLog auditLog = new AuditLog(
                "PATIENT_CREATED",
                event.toString(),
                String.valueOf(event.getPatientId()),
                LocalDateTime.now()
        );

        auditLogRepository.save(auditLog);

        logger.info(
                "Saved audit log for eventType={}, actorId={}, topic={}, partition={}, offset={}",
                "PATIENT_CREATED",
                event.getPatientId(),
                record.topic(),
                record.partition(),
                record.offset()
        );
    }
}