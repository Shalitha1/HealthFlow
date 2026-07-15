package com.pm.billingservice.dto;
import com.pm.billingservice.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public record InvoiceResponse(
        Long id, String billingAccountId, String patientId, Long appointmentId, String invoiceNumber,
        BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal balance, InvoiceStatus status,
        LocalDate dueDate, OffsetDateTime createdAt, List<Item> items, List<PaymentView> payments) {
    public record Item(Long id, String description, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
    public record PaymentView(Long id, BigDecimal amount, String paymentMethod, OffsetDateTime paidAt, String reference) {}
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(), invoice.getBillingAccount().getId(), invoice.getBillingAccount().getPatientId(),
                invoice.getAppointmentId(), invoice.getInvoiceNumber(), invoice.getTotalAmount(), invoice.paidAmount(), invoice.balance(),
                invoice.getStatus(), invoice.getDueDate(), invoice.getCreatedAt(),
                invoice.getItems().stream().map(i -> new Item(i.getId(), i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal())).toList(),
                invoice.getPayments().stream().map(p -> new PaymentView(p.getId(), p.getAmount(), p.getPaymentMethod(), p.getPaidAt(), p.getReference())).toList());
    }
}
