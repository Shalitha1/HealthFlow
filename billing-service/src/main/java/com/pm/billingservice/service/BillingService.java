package com.pm.billingservice.service;

import com.pm.billingservice.dto.*;
import com.pm.billingservice.event.BillingEventProducer;
import com.pm.billingservice.exception.ApiException;
import com.pm.billingservice.model.*;
import com.pm.billingservice.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class BillingService {
    private final BillingAccountRepository accounts; private final InvoiceRepository invoices;
    private final PaymentRepository payments; private final BillingEventProducer events;
    public BillingService(BillingAccountRepository accounts, InvoiceRepository invoices, PaymentRepository payments, BillingEventProducer events) {
        this.accounts = accounts; this.invoices = invoices; this.payments = payments; this.events = events;
    }

    @Transactional
    public BillingAccountResponse createAccount(CreateBillingRequest request, String actorId) {
        Optional<BillingAccount> existing = accounts.findByPatientId(request.patientId().trim());
        if (existing.isPresent()) return BillingAccountResponse.from(existing.get());
        BillingAccount account = new BillingAccount(request.patientId().trim());
        try { accounts.saveAndFlush(account); }
        catch (DataIntegrityViolationException exception) { return BillingAccountResponse.from(accounts.findByPatientId(request.patientId().trim()).orElseThrow()); }
        events.publish("BillingAccountCreated", account, null, null, null, actorId);
        return BillingAccountResponse.from(account);
    }

    @Transactional
    public PatientBillingResponse patientBilling(String patientId) {
        BillingAccount account = accounts.findByPatientId(patientId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "BILLING_ACCOUNT_NOT_FOUND", "No billing account exists for patient " + patientId));
        List<Invoice> records = invoices.findDistinctByBillingAccountPatientIdOrderByCreatedAtDesc(patientId);
        records.forEach(this::refreshStatus);
        return patientResponse(account, records);
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, String actorId) {
        BillingAccount account = accounts.findById(request.billingAccountId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "BILLING_ACCOUNT_NOT_FOUND", "Billing account " + request.billingAccountId() + " was not found"));
        if (account.getStatus() != BillingAccountStatus.ACTIVE) throw new ApiException(HttpStatus.CONFLICT, "BILLING_ACCOUNT_CLOSED", "Invoices cannot be created for a closed account");
        InvoiceStatus requestedStatus = request.status() == null ? InvoiceStatus.ISSUED : request.status();
        if (requestedStatus != InvoiceStatus.DRAFT && requestedStatus != InvoiceStatus.ISSUED) throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_INITIAL_STATUS", "A new invoice must be DRAFT or ISSUED");
        Invoice invoice = new Invoice(account, request.appointmentId(), request.dueDate(), requestedStatus);
        request.items().forEach(item -> invoice.addItem(item.description().trim(), item.quantity(), item.unitPrice()));
        invoices.saveAndFlush(invoice); invoice.assignNumber(); invoices.saveAndFlush(invoice);
        events.publish(requestedStatus == InvoiceStatus.DRAFT ? "InvoiceCreated" : "InvoiceIssued", account, invoice, null, invoice.getTotalAmount(), actorId);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public List<InvoiceResponse> list(InvoiceStatus status, String patientId) {
        Specification<Invoice> spec = (root, query, builder) -> builder.conjunction();
        if (patientId != null && !patientId.isBlank()) spec = spec.and((root, query, builder) -> builder.equal(root.get("billingAccount").get("patientId"), patientId));
        List<Invoice> records = invoices.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        records.forEach(this::refreshStatus);
        return records.stream().filter(i -> status == null || i.getStatus() == status).map(InvoiceResponse::from).toList();
    }

    @Transactional
    public InvoiceResponse get(Long id) { Invoice invoice = findDetailed(id); refreshStatus(invoice); return InvoiceResponse.from(invoice); }

    @Transactional
    public InvoiceResponse recordPayment(Long id, PaymentRequest request, String actorId) {
        Invoice invoice = invoices.findLockedById(id).orElseThrow(() -> notFound(id));
        refreshStatus(invoice);
        if (invoice.getStatus() == InvoiceStatus.DRAFT) throw new ApiException(HttpStatus.CONFLICT, "INVOICE_NOT_ISSUED", "Payments cannot be recorded against a draft invoice");
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) throw new ApiException(HttpStatus.CONFLICT, "INVOICE_CANCELLED", "Payments cannot be recorded against a cancelled invoice");
        if (invoice.getStatus() == InvoiceStatus.PAID) throw new ApiException(HttpStatus.CONFLICT, "INVOICE_ALREADY_PAID", "The invoice is already paid");
        BigDecimal amount = request.amount().setScale(2, java.math.RoundingMode.HALF_UP);
        if (amount.compareTo(invoice.balance()) > 0) throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_EXCEEDS_BALANCE", "Payment cannot exceed the outstanding balance of " + invoice.balance());
        Payment payment = new Payment(invoice, amount, request.paymentMethod().trim().toUpperCase(Locale.ROOT), clean(request.reference()));
        payments.save(payment); invoice.addPayment(payment); applyPaymentStatus(invoice); invoices.saveAndFlush(invoice);
        events.publish("PaymentRecorded", invoice.getBillingAccount(), invoice, payment, amount, actorId);
        if (invoice.getStatus() == InvoiceStatus.PAID) events.publish("InvoicePaid", invoice.getBillingAccount(), invoice, payment, invoice.getTotalAmount(), actorId);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public BillingStatisticsResponse statistics() {
        List<Invoice> records = invoices.findAll(); records.forEach(this::refreshStatus);
        BigDecimal outstanding = records.stream().filter(this::isOutstanding).map(Invoice::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = records.stream().map(Invoice::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BillingStatisticsResponse(records.size(), records.stream().filter(this::isOutstanding).count(),
                records.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE).count(), outstanding, received);
    }

    private Invoice findDetailed(Long id) { return invoices.findDetailedById(id).orElseThrow(() -> notFound(id)); }
    private ApiException notFound(Long id) { return new ApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice " + id + " was not found"); }
    private void refreshStatus(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.PAID) return;
        applyPaymentStatus(invoice);
    }
    private void applyPaymentStatus(Invoice invoice) {
        if (invoice.paidAmount().compareTo(invoice.getTotalAmount()) >= 0) invoice.changeStatus(InvoiceStatus.PAID);
        else if (invoice.getDueDate().isBefore(LocalDate.now())) invoice.changeStatus(InvoiceStatus.OVERDUE);
        else if (invoice.paidAmount().signum() > 0) invoice.changeStatus(InvoiceStatus.PARTIALLY_PAID);
        else invoice.changeStatus(InvoiceStatus.ISSUED);
    }
    private boolean isOutstanding(Invoice invoice) { return invoice.getStatus() == InvoiceStatus.ISSUED || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID || invoice.getStatus() == InvoiceStatus.OVERDUE; }
    private PatientBillingResponse patientResponse(BillingAccount account, List<Invoice> records) {
        BigDecimal billed = records.stream().filter(i -> i.getStatus() != InvoiceStatus.CANCELLED).map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = records.stream().map(Invoice::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = records.stream().filter(this::isOutstanding).map(Invoice::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PatientBillingResponse(BillingAccountResponse.from(account), billed, paid, balance, records.stream().map(InvoiceResponse::from).toList());
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
