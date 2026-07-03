package com.pm.billingservice.controller;

import com.pm.billingservice.dto.CreateBillingRequest;
import com.pm.billingservice.dto.CreateBillingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final Logger logger = LoggerFactory.getLogger(BillingController.class);

    @PostMapping("/accounts")
    public ResponseEntity<CreateBillingResponse> createBillingAccount(@RequestBody CreateBillingRequest request) {
        String accountId = UUID.randomUUID().toString();

        logger.info("Creating billing account for patient: {}", request.patientId());
        logger.info("Patient name: {}", request.name());
        logger.info("Patient email: {}", request.email());
        logger.info("Assigned billing account ID: {}", accountId);

        CreateBillingResponse response = new CreateBillingResponse(accountId, "ACTIVE");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Billing Service is healthy");
    }
}
