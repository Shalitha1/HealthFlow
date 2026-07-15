package com.pm.billingservice.service;

import com.pm.billingservice.dto.PaymentRequest;
import com.pm.billingservice.event.BillingEventProducer;
import com.pm.billingservice.exception.ApiException;
import com.pm.billingservice.model.*;
import com.pm.billingservice.repository.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class BillingServiceTests {
    private BillingService service;
    private Invoice invoice;
    private int savedPayments;
    private final List<String> eventTypes = new ArrayList<>();

    @BeforeEach void setUp() {
        invoice = new Invoice(new BillingAccount("42"), 7L, LocalDate.now().plusDays(7), InvoiceStatus.ISSUED);
        invoice.addItem("Consultation", 2, new BigDecimal("50.00"));
        invoice.addItem("Supplies", 1, new BigDecimal("25.00"));
        InvoiceRepository invoices = proxy(InvoiceRepository.class, (method, args) -> switch (method) {
            case "findLockedById" -> Optional.of(invoice);
            case "saveAndFlush" -> args[0];
            default -> null;
        });
        PaymentRepository payments = proxy(PaymentRepository.class, (method, args) -> {
            if (method.equals("save")) { savedPayments++; return args[0]; }
            return null;
        });
        BillingAccountRepository accounts = proxy(BillingAccountRepository.class, (method, args) -> null);
        BillingEventProducer events = new BillingEventProducer(null, null, "billing-events") {
            @Override public void publish(String type, BillingAccount account, Invoice invoice, Payment payment,
                                          BigDecimal amount, String actorId) { eventTypes.add(type); }
        };
        service = new BillingService(accounts, invoices, payments, events);
    }

    @Test void calculatesPartialAndPaidStatusesFromStoredPayments() {
        var partial = service.recordPayment(99L, new PaymentRequest(new BigDecimal("25.00"), "card", "P-1"), "admin");
        assertEquals(new BigDecimal("125.00"), partial.totalAmount());
        assertEquals(new BigDecimal("100.00"), partial.balance());
        assertEquals(InvoiceStatus.PARTIALLY_PAID, partial.status());

        var paid = service.recordPayment(99L, new PaymentRequest(new BigDecimal("100.00"), "bank_transfer", "P-2"), "admin");
        assertEquals(new BigDecimal("125.00"), paid.paidAmount());
        assertEquals(new BigDecimal("0.00"), paid.balance());
        assertEquals(InvoiceStatus.PAID, paid.status());
        assertTrue(eventTypes.contains("InvoicePaid"));
        assertEquals(2, savedPayments);
    }

    @Test void rejectsPaymentAboveCurrentBalance() {
        ApiException exception = assertThrows(ApiException.class, () -> service.recordPayment(99L,
                new PaymentRequest(new BigDecimal("125.01"), "cash", null), "admin"));
        assertEquals("PAYMENT_EXCEEDS_BALANCE", exception.getErrorCode());
        assertEquals(409, exception.getStatus().value());
        assertEquals(0, savedPayments);
    }

    private interface Invocation { Object call(String method, Object[] args); }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (value, method, args) -> invocation.call(method.getName(), args == null ? new Object[0] : args));
    }
}
