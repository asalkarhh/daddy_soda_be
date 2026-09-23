package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.ManualDailyFlavourSales;
import com.asalkartechworks.daddysodahouse.entity.ManualSalesQuantityUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManualDailyFlavourSalesRepository extends JpaRepository<ManualDailyFlavourSales, UUID> {

    boolean existsByMachineIdAndProductIdAndSalesDate(UUID machineId, UUID productId, LocalDate salesDate);

    Optional<ManualDailyFlavourSales> findByMachineIdAndProductIdAndSalesDate(UUID machineId, UUID productId, LocalDate salesDate);

    @Query("SELECT s FROM ManualDailyFlavourSales s " +
           "JOIN FETCH s.franchise f " +
           "JOIN FETCH s.machine m " +
           "JOIN FETCH s.product p " +
           "WHERE (:franchiseId IS NULL OR f.id = :franchiseId) " +
           "AND (:machineId IS NULL OR m.id = :machineId) " +
           "AND (:productId IS NULL OR p.id = :productId) " +
           "AND (:quantityUnit IS NULL OR s.quantityUnit = :quantityUnit) " +
           "AND (cast(:fromDate as date) IS NULL OR s.salesDate >= :fromDate) " +
           "AND (cast(:toDate as date) IS NULL OR s.salesDate <= :toDate)")
    Page<ManualDailyFlavourSales> searchManualSales(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("productId") UUID productId,
            @Param("quantityUnit") ManualSalesQuantityUnit quantityUnit,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("SELECT p.id, p.itemCode, p.flavourName, " +
           "SUM(CASE WHEN s.quantityUnit = 'GLASSES' THEN s.quantity ELSE 0 END), " +
           "SUM(CASE WHEN s.quantityUnit = 'ML' THEN s.quantity ELSE 0 END) " +
           "FROM ManualDailyFlavourSales s " +
           "JOIN s.product p " +
           "WHERE (:franchiseId IS NULL OR s.franchise.id = :franchiseId) " +
           "AND (:machineId IS NULL OR s.machine.id = :machineId) " +
           "AND (:productId IS NULL OR s.product.id = :productId) " +
           "AND (cast(:fromDate as date) IS NULL OR s.salesDate >= :fromDate) " +
           "AND (cast(:toDate as date) IS NULL OR s.salesDate <= :toDate) " +
           "GROUP BY p.id, p.itemCode, p.flavourName " +
           "ORDER BY p.flavourName ASC")
    List<Object[]> findFlavourSalesTotals(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("productId") UUID productId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM ManualDailyFlavourSales s " +
           "WHERE (:franchiseId IS NULL OR s.franchise.id = :franchiseId) " +
           "AND (:machineId IS NULL OR s.machine.id = :machineId) " +
           "AND (:productId IS NULL OR s.product.id = :productId) " +
           "AND (:quantityUnit IS NULL OR s.quantityUnit = :quantityUnit) " +
           "AND (cast(:fromDate as date) IS NULL OR s.salesDate >= :fromDate) " +
           "AND (cast(:toDate as date) IS NULL OR s.salesDate <= :toDate)")
    BigDecimal sumQuantity(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("productId") UUID productId,
            @Param("quantityUnit") ManualSalesQuantityUnit quantityUnit,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT s FROM ManualDailyFlavourSales s " +
           "JOIN FETCH s.franchise f " +
           "JOIN FETCH s.machine m " +
           "JOIN FETCH s.product p " +
           "WHERE (:franchiseId IS NULL OR f.id = :franchiseId) " +
           "AND (:machineId IS NULL OR m.id = :machineId) " +
           "AND (:productId IS NULL OR p.id = :productId) " +
           "AND (cast(:fromDate as date) IS NULL OR s.salesDate >= :fromDate) " +
           "AND (cast(:toDate as date) IS NULL OR s.salesDate <= :toDate) " +
           "ORDER BY s.salesDate DESC, m.machineCode ASC, p.flavourName ASC")
    List<ManualDailyFlavourSales> findReportSales(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("productId") UUID productId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
