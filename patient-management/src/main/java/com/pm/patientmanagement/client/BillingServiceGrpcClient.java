package com.pm.patientmanagement.client;

import com.pm.billing.grpc.BillingServiceGrpc;
import com.pm.billing.grpc.CreateBillingRequest;
import com.pm.billing.grpc.CreateBillingResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BillingServiceGrpcClient {

    @Value("${grpc.billing-service.host:localhost}")
    private String billingHost;

    @Value("${grpc.billing-service.port:9090}")
    private int billingPort;

    /**
     * Calls the Billing Service to create a billing account for a new patient
     */
    public String createBillingAccount(String patientId, String name, String email) {
        try {
            // Create a channel to the Billing Service
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(billingHost, billingPort)
                    .usePlaintext() // Use plaintext (not TLS) for simplicity
                    .build();

            // Create a blocking stub (synchronous call)
            BillingServiceGrpc.BillingServiceBlockingStub stub =
                    BillingServiceGrpc.newBlockingStub(channel);

            // Build the request
            CreateBillingRequest request = CreateBillingRequest.newBuilder()
                    .setPatientId(patientId)
                    .setName(name)
                    .setEmail(email)
                    .build();

            // Make the RPC call
            CreateBillingResponse response = stub.createBillingAccount(request);

            // Extract the account ID
            String accountId = response.getAccountId();
            System.out.println("Billing account created: " + accountId + " with status: " + response.getStatus());

            // Close the channel
            channel.shutdown();

            return accountId;

        } catch (Exception e) {
            System.err.println("Error calling Billing Service: " + e.getMessage());
            throw new RuntimeException("Failed to create billing account via gRPC", e);
        }
    }
}