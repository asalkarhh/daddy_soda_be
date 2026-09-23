package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.InventoryStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, UUID> {

    Optional<InventoryStock> findByProductId(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventoryStock s WHERE s.product.id = :productId")
    Optional<InventoryStock> findByProductIdWithLock(@Param("productId") UUID productId);
}
