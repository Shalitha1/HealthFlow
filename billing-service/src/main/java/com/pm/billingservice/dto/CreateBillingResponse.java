package com.pm.billingservice.dto;

public record CreateBillingResponse(
        String accountId,
        String status
) {
}
