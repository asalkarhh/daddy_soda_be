package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.IotEventItem;
import com.asalkartechworks.daddysodahouse.entity.IotItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IotEventItemRepository extends JpaRepository<IotEventItem, UUID> {

    @Query("SELECT item.product.id, item.itemCode, COALESCE(item.product.flavourName, item.itemName), SUM(item.quantityMl) " +
           "FROM IotEventItem item " +
           "JOIN item.iotEvent e " +
           "WHERE (:franchiseId IS NULL OR e.franchise.id = :franchiseId) " +
           "AND (:machineId IS NULL OR e.machine.id = :machineId) " +
           "AND (:productId IS NULL OR item.product.id = :productId) " +
           "AND (cast(:fromDateTime as timestamp) IS NULL OR e.eventTime >= :fromDateTime) " +
           "AND (cast(:toDateTime as timestamp) IS NULL OR e.eventTime <= :toDateTime) " +
           "AND e.eventStatus = 'SUCCESS' AND e.eventType = 'DRINK_DISPENSED' AND item.itemType = 'SYRUP' " +
           "GROUP BY item.product.id, item.itemCode, COALESCE(item.product.flavourName, item.itemName) " +
           "ORDER BY SUM(item.quantityMl) DESC")
    List<Object[]> findFlavourSyrupUsage(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("productId") UUID productId,
            @Param("fromDateTime") Instant fromDateTime,
            @Param("toDateTime") Instant toDateTime
    );

    @Query("SELECT e.machine.machineCode, e.machine.machineName, " +
           "SUM(CASE WHEN item.itemType = 'SYRUP' THEN item.quantityMl ELSE 0 END), " +
           "SUM(CASE WHEN item.itemType = 'SODA' THEN item.quantityMl ELSE 0 END) " +
           "FROM IotEventItem item " +
           "JOIN item.iotEvent e " +
           "WHERE (:franchiseId IS NULL OR e.franchise.id = :franchiseId) " +
           "AND (:machineId IS NULL OR e.machine.id = :machineId) " +
           "AND (:productId IS NULL OR item.product.id = :productId) " +
           "AND (cast(:fromDateTime as timestamp) IS NULL OR e.eventTime >= :fromDateTime) " +
           "AND (cast(:toDateTime as timestamp) IS NULL OR e.eventTime <= :toDateTime) " +
           "AND e.eventStatus = 'SUCCESS' AND e.eventType = 'DRINK_DISPENSED' " +
           "GROUP BY e.machine.machineCode, e.machine.machineName " +
           "ORDER BY e.machine.machineCode ASC")
    List<Object[]> findMachineUsage(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("productId") UUID productId,
            @Param("fromDateTime") Instant fromDateTime,
            @Param("toDateTime") Instant toDateTime
    );

    @Query("SELECT item FROM IotEventItem item " +
           "JOIN FETCH item.iotEvent e " +
           "JOIN FETCH e.machine m " +
           "JOIN FETCH e.franchise f " +
           "LEFT JOIN FETCH item.product p " +
           "WHERE (:franchiseId IS NULL OR f.id = :franchiseId) " +
           "AND (:machineId IS NULL OR m.id = :machineId) " +
           "AND (:productId IS NULL OR p.id = :productId) " +
           "AND (cast(:fromDateTime as timestamp) IS NULL OR e.eventTime >= :fromDateTime) " +
           "AND (cast(:toDateTime as timestamp) IS NULL OR e.eventTime <= :toDateTime) " +
           "ORDER BY e.eventTime DESC, item.lineNo ASC NULLS LAST")
    List<IotEventItem> findReportItems(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("productId") UUID productId,
            @Param("fromDateTime") Instant fromDateTime,
            @Param("toDateTime") Instant toDateTime
    );
}
