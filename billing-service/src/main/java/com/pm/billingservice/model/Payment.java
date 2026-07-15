package com.pm.billingservice.model;

import jakarta.persistence.*;
import java.math.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id") private Invoice invoice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "payment_method", nullable = false) private String paymentMethod;
    @Column(name = "paid_at", nullable = false) private OffsetDateTime paidAt;
    private String reference;
    protected Payment() {}
    public Payment(Invoice invoice, BigDecimal amount, String paymentMethod, String reference) {
        this.invoice = invoice; this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.paymentMethod = paymentMethod; this.reference = reference; this.paidAt = OffsetDateTime.now();
    }
    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public String getReference() { return reference; }
}
