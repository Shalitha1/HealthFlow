package com.pm.patientmanagement.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BillingServiceClient {

    private final RestClient restClient;

    public BillingServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${billing-service.url:http://localhost:8081}") String billingServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(billingServiceUrl)
                .build();
    }

    public String createBillingAccount(String patientId, String name, String email) {
        CreateBillingResponse response = restClient.post()
                .uri("/api/billing/accounts")
                .body(new CreateBillingRequest(patientId, name, email))
                .retrieve()
                .body(CreateBillingResponse.class);

        if (response == null) {
            throw new IllegalStateException("Billing service returned an empty response");
        }

        return response.accountId();
    }

    private record CreateBillingRequest(String patientId, String name, String email) {
    }

    private record CreateBillingResponse(String accountId, String status) {
    }
}
