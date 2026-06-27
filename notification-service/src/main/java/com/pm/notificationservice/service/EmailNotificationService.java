package com.pm.notificationservice.service;

import com.pm.event.PatientCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    public void sendWelcomeEmail(PatientCreatedEvent event) {
        logger.info("Would send welcome email to {} for patient {}", event.getEmail(), event.getPatientId());
    }
}