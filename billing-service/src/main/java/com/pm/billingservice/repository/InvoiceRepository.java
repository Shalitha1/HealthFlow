package com.pm.billingservice.repository;

import com.pm.billingservice.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    @EntityGraph(attributePaths = {"billingAccount", "items", "payments"})
    @Query("select distinct i from Invoice i where i.id = :id") Optional<Invoice> findDetailedById(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id") Optional<Invoice> findLockedById(@Param("id") Long id);
    @EntityGraph(attributePaths = {"billingAccount", "items", "payments"})
    List<Invoice> findDistinctByBillingAccountPatientIdOrderByCreatedAtDesc(String patientId);
}
