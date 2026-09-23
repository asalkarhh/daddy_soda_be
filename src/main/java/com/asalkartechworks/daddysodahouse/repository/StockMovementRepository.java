package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.StockMovement;
import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findTop20ByProductIdOrderByMovementAtDescCreatedAtDesc(UUID productId);

    @Query("SELECT MAX(m.movementAt) FROM StockMovement m WHERE m.product.id = :productId")
    Optional<Instant> findLatestMovementAtByProductId(@Param("productId") UUID productId);

    @Query("SELECT m FROM StockMovement m WHERE " +
           "(:productId IS NULL OR m.product.id = :productId) AND " +
           "(:movementType IS NULL OR m.movementType = :movementType) AND " +
           "(:referenceType IS NULL OR LOWER(m.referenceType) = LOWER(:referenceType)) AND " +
           "(:fromInstant IS NULL OR m.movementAt >= :fromInstant) AND " +
           "(:toInstant IS NULL OR m.movementAt <= :toInstant)")
    Page<StockMovement> searchStockMovements(
            @Param("productId") UUID productId,
            @Param("movementType") StockMovementType movementType,
            @Param("referenceType") String referenceType,
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant,
            Pageable pageable
    );
}
