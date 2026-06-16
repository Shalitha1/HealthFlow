package com.pm.billingservice.grpc;

import com.pm.billing.grpc.BillingServiceGrpc;
import com.pm.billing.grpc.CreateBillingRequest;
import com.pm.billing.grpc.CreateBillingResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.UUID;

@GrpcService
public class BillingServiceImpl extends BillingServiceGrpc.BillingServiceImplBase {

    @Override
    public void createBillingAccount(CreateBillingRequest request,
                                     StreamObserver<CreateBillingResponse> responseObserver) {

        try {
            // Generate a unique account ID
            String accountId = UUID.randomUUID().toString();

            // In a real system, you'd save this to a database
            System.out.println("Creating billing account for patient: " + request.getPatientId());
            System.out.println("Patient Name: " + request.getName());
            System.out.println("Patient Email: " + request.getEmail());
            System.out.println("Assigned Account ID: " + accountId);

            // Build response
            CreateBillingResponse response = CreateBillingResponse.newBuilder()
                    .setAccountId(accountId)
                    .setStatus("ACTIVE")
                    .build();

            // Send response back to client
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(new RuntimeException("Failed to create billing account: " + e.getMessage()));
        }
    }
}