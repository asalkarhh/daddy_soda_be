package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.SalesOrder;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
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
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

    Optional<SalesOrder> findByOrderNumber(String orderNumber);

    @Query("SELECT o.orderNumber FROM SalesOrder o " +
           "WHERE o.orderNumber LIKE CONCAT(:prefix, '%') " +
           "ORDER BY o.orderNumber DESC " +
           "LIMIT 1")
    Optional<String> findMaxOrderNumberWithPrefix(@Param("prefix") String prefix);

    @Query("SELECT o FROM SalesOrder o WHERE " +
           "(:franchiseId IS NULL OR o.franchise.id = :franchiseId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:orderNumber IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', CAST(:orderNumber AS String), '%'))) AND " +
           "(:fromDate IS NULL OR o.orderDate >= :fromDate) AND " +
           "(:toDate IS NULL OR o.orderDate <= :toDate)")
    Page<SalesOrder> searchOrders(
            @Param("franchiseId") UUID franchiseId,
            @Param("status") SalesOrderStatus status,
            @Param("orderNumber") String orderNumber,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    long countByStatus(SalesOrderStatus status);

    long countByStatusAndOrderDateBetween(SalesOrderStatus status, LocalDate start, LocalDate end);

    long countByFranchiseIdAndStatus(UUID franchiseId, SalesOrderStatus status);

    @Query("""
        SELECT o FROM SalesOrder o
        WHERE o.franchise.id = :franchiseId
        ORDER BY o.orderDate DESC, o.createdAt DESC
    """)
    java.util.List<SalesOrder> findLatestOrdersByFranchiseId(@Param("franchiseId") UUID franchiseId, Pageable pageable);

    @Query("""
        SELECT DISTINCT o FROM SalesOrder o
        JOIN FETCH o.franchise f
        LEFT JOIN FETCH o.items items
        LEFT JOIN FETCH items.product p
        WHERE (:franchiseId IS NULL OR f.id = :franchiseId)
          AND (:status IS NULL OR o.status = :status)
          AND o.orderDate >= :fromDate
          AND o.orderDate <= :toDate
        ORDER BY o.orderDate DESC, o.orderNumber DESC
    """)
    java.util.List<SalesOrder> findMonthlyOrdersForReport(
            @Param("franchiseId") UUID franchiseId,
            @Param("status") SalesOrderStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
