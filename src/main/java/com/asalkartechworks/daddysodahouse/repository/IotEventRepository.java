package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.IotEvent;
import com.asalkartechworks.daddysodahouse.entity.IotEventStatus;
import com.asalkartechworks.daddysodahouse.entity.IotEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IotEventRepository extends JpaRepository<IotEvent, UUID> {

    Optional<IotEvent> findByExternalEventId(String externalEventId);

    boolean existsByExternalEventId(String externalEventId);

    @Query("SELECT e FROM IotEvent e LEFT JOIN FETCH e.items WHERE e.id = :id")
    Optional<IotEvent> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT DISTINCT e FROM IotEvent e " +
           "LEFT JOIN e.items item " +
           "WHERE (:franchiseId IS NULL OR e.franchise.id = :franchiseId) " +
           "AND (:machineId IS NULL OR e.machine.id = :machineId) " +
           "AND (:deviceId IS NULL OR LOWER(e.machine.deviceId) = LOWER(:deviceId)) " +
           "AND (:eventType IS NULL OR e.eventType = :eventType) " +
           "AND (:eventStatus IS NULL OR e.eventStatus = :eventStatus) " +
           "AND (:productId IS NULL OR item.product.id = :productId) " +
           "AND (cast(:fromDateTime as timestamp) IS NULL OR e.eventTime >= :fromDateTime) " +
           "AND (cast(:toDateTime as timestamp) IS NULL OR e.eventTime <= :toDateTime)")
    Page<IotEvent> searchEvents(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("deviceId") String deviceId,
            @Param("eventType") IotEventType eventType,
            @Param("eventStatus") IotEventStatus eventStatus,
            @Param("productId") UUID productId,
            @Param("fromDateTime") Instant fromDateTime,
            @Param("toDateTime") Instant toDateTime,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(e.syrupQuantityMl), 0) FROM IotEvent e " +
           "WHERE (:franchiseId IS NULL OR e.franchise.id = :franchiseId) " +
           "AND (:machineId IS NULL OR e.machine.id = :machineId) " +
           "AND (cast(:fromDateTime as timestamp) IS NULL OR e.eventTime >= :fromDateTime) " +
           "AND (cast(:toDateTime as timestamp) IS NULL OR e.eventTime <= :toDateTime) " +
           "AND e.eventStatus = 'SUCCESS' AND e.eventType = 'DRINK_DISPENSED'")
    BigDecimal sumTotalSyrupMl(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("fromDateTime") Instant fromDateTime,
            @Param("toDateTime") Instant toDateTime
    );

    @Query("SELECT COALESCE(SUM(e.rawSodaQuantityMl), 0) FROM IotEvent e " +
           "WHERE (:franchiseId IS NULL OR e.franchise.id = :franchiseId) " +
           "AND (:machineId IS NULL OR e.machine.id = :machineId) " +
           "AND (cast(:fromDateTime as timestamp) IS NULL OR e.eventTime >= :fromDateTime) " +
           "AND (cast(:toDateTime as timestamp) IS NULL OR e.eventTime <= :toDateTime) " +
           "AND e.eventStatus = 'SUCCESS' AND e.eventType = 'DRINK_DISPENSED'")
    BigDecimal sumTotalRawSodaMl(
            @Param("franchiseId") UUID franchiseId,
            @Param("machineId") UUID machineId,
            @Param("fromDateTime") Instant fromDateTime,
            @Param("toDateTime") Instant toDateTime
    );
}
