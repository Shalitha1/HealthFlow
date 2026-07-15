package com.pm.billingservice.dto;
import java.math.BigDecimal;
import java.util.List;
public record PatientBillingResponse(BillingAccountResponse account, BigDecimal totalBilled, BigDecimal totalPaid, BigDecimal outstandingBalance, List<InvoiceResponse> invoices) {}
