package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.Invoice;
import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.PaymentTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsBySalesOrderId(UUID salesOrderId);

    Optional<Invoice> findBySalesOrderId(UUID salesOrderId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT MAX(i.invoiceNumber) FROM Invoice i WHERE i.invoiceNumber LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxInvoiceNumberWithPrefix(@Param("prefix") String prefix);

    @Query("""
        SELECT i FROM Invoice i
        JOIN i.salesOrder so
        JOIN so.franchise f
        WHERE (:franchiseId IS NULL OR f.id = :franchiseId)
          AND (:salesOrderId IS NULL OR so.id = :salesOrderId)
          AND (:invoiceNumber IS NULL OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :invoiceNumber, '%')))
          AND (:invoiceType IS NULL OR i.invoiceType = :invoiceType)
          AND (:paymentStatus IS NULL OR i.paymentStatus = :paymentStatus)
          AND (:paymentTerm IS NULL OR i.paymentTerm = :paymentTerm)
          AND (:fromDate IS NULL OR i.invoiceDate >= :fromDate)
          AND (:toDate IS NULL OR i.invoiceDate <= :toDate)
          AND (:dueFrom IS NULL OR i.dueDate >= :dueFrom)
          AND (:dueTo IS NULL OR i.dueDate <= :dueTo)
          AND (:overdueOnly IS NULL OR (:overdueOnly = true AND i.paymentStatus = 'OVERDUE') OR :overdueOnly = false)
    """)
    Page<Invoice> searchInvoices(
            @Param("franchiseId") UUID franchiseId,
            @Param("salesOrderId") UUID salesOrderId,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("invoiceType") InvoiceType invoiceType,
            @Param("paymentStatus") InvoicePaymentStatus paymentStatus,
            @Param("paymentTerm") PaymentTerm paymentTerm,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo,
            @Param("overdueOnly") Boolean overdueOnly,
            Pageable pageable);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.balanceAmount > 0
          AND i.dueDate IS NOT NULL
          AND i.dueDate < :today
          AND i.paymentStatus != 'OVERDUE'
    """)
    List<Invoice> findInvoicesBecomingOverdue(@Param("today") LocalDate today);
}
