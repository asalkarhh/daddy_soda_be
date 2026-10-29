package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>, JpaSpecificationExecutor<StockMovement> {

    List<StockMovement> findTop20ByProductIdOrderByMovementAtDescCreatedAtDesc(UUID productId);

    @Query("SELECT MAX(m.movementAt) FROM StockMovement m WHERE m.product.id = :productId")
    Optional<Instant> findLatestMovementAtByProductId(@Param("productId") UUID productId);
}
