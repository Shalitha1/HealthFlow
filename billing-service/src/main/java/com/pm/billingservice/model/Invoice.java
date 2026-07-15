package com.pm.billingservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "billing_account_id") private BillingAccount billingAccount;
    @Column(name = "appointment_id") private Long appointmentId;
    @Column(name = "invoice_number", nullable = false, unique = true) private String invoiceNumber;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InvoiceStatus status;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("id ASC") private Set<InvoiceItem> items = new LinkedHashSet<>();
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL) @OrderBy("paidAt ASC") private Set<Payment> payments = new LinkedHashSet<>();

    protected Invoice() {}
    public Invoice(BillingAccount account, Long appointmentId, LocalDate dueDate, InvoiceStatus status) {
        this.billingAccount = account; this.appointmentId = appointmentId; this.dueDate = dueDate;
        this.status = status; this.createdAt = OffsetDateTime.now();
        this.invoiceNumber = "PENDING-" + UUID.randomUUID(); this.totalAmount = BigDecimal.ZERO;
    }
    public void addItem(String description, Integer quantity, BigDecimal unitPrice) {
        InvoiceItem item = new InvoiceItem(this, description, quantity, unitPrice); items.add(item);
        totalAmount = totalAmount.add(item.getSubtotal());
    }
    public void assignNumber() { this.invoiceNumber = "HF-INV-" + String.format("%08d", id); }
    public void addPayment(Payment payment) { payments.add(payment); }
    public void changeStatus(InvoiceStatus status) { this.status = status; }
    public BigDecimal paidAmount() { return payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public BigDecimal balance() { return totalAmount.subtract(paidAmount()).max(BigDecimal.ZERO); }
    public Long getId() { return id; }
    public BillingAccount getBillingAccount() { return billingAccount; }
    public Long getAppointmentId() { return appointmentId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Set<InvoiceItem> getItems() { return items; }
    public Set<Payment> getPayments() { return payments; }
}
