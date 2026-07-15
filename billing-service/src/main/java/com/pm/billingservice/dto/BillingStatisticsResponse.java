package com.pm.billingservice.dto;
import java.math.BigDecimal;
public record BillingStatisticsResponse(long totalInvoices, long outstandingInvoices, long overdueInvoices, BigDecimal outstandingBalance, BigDecimal paymentsReceived) {}
