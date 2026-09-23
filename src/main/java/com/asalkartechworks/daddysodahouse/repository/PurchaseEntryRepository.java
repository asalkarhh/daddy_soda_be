package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.PurchaseEntry;
import com.asalkartechworks.daddysodahouse.entity.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseEntryRepository extends JpaRepository<PurchaseEntry, UUID> {

    Optional<PurchaseEntry> findByPurchaseNumber(String purchaseNumber);

    boolean existsBySupplierIdAndSupplierBillNumberAndStatus(UUID supplierId, String supplierBillNumber, PurchaseStatus status);

    @Query("SELECT p.purchaseNumber FROM PurchaseEntry p " +
           "WHERE p.purchaseNumber LIKE CONCAT(:prefix, '%') " +
           "ORDER BY p.purchaseNumber DESC " +
           "LIMIT 1")
    Optional<String> findMaxPurchaseNumberWithPrefix(@Param("prefix") String prefix);

    @Query("SELECT p FROM PurchaseEntry p WHERE " +
           "(:supplierId IS NULL OR p.supplier.id = :supplierId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:purchaseNumber IS NULL OR LOWER(p.purchaseNumber) LIKE LOWER(CONCAT('%', :purchaseNumber, '%'))) AND " +
           "(:supplierBillNumber IS NULL OR LOWER(p.supplierBillNumber) LIKE LOWER(CONCAT('%', :supplierBillNumber, '%'))) AND " +
           "(:fromDate IS NULL OR p.billDate >= :fromDate) AND " +
           "(:toDate IS NULL OR p.billDate <= :toDate)")
    Page<PurchaseEntry> searchPurchases(
            @Param("supplierId") UUID supplierId,
            @Param("status") PurchaseStatus status,
            @Param("purchaseNumber") String purchaseNumber,
            @Param("supplierBillNumber") String supplierBillNumber,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    long countByBillDateBetweenAndStatus(LocalDate fromDate, LocalDate toDate, PurchaseStatus status);

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0) FROM PurchaseItem i
        JOIN i.purchaseEntry pe
        WHERE pe.billDate >= :fromDate
          AND pe.billDate <= :toDate
          AND pe.status = :status
    """)
    java.math.BigDecimal sumQuantityByBillDateBetweenAndStatus(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") PurchaseStatus status);

    @Query("""
        SELECT DISTINCT pe FROM PurchaseEntry pe
        JOIN FETCH pe.supplier s
        LEFT JOIN FETCH pe.items items
        LEFT JOIN FETCH items.product p
        WHERE (:supplierId IS NULL OR s.id = :supplierId)
          AND pe.billDate >= :fromDate
          AND pe.billDate <= :toDate
        ORDER BY pe.billDate DESC, pe.purchaseNumber DESC
    """)
    java.util.List<PurchaseEntry> findPurchasesForReport(
            @Param("supplierId") UUID supplierId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
