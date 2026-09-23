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

    @Query("SELECT COUNT(s) FROM InventoryStock s WHERE s.product.isActive = true AND s.availableQuantity > 0")
    long countActiveProductsInStock();

    @Query("""
        SELECT COUNT(p) FROM Product p
        WHERE p.isActive = true
          AND (
            NOT EXISTS (SELECT 1 FROM InventoryStock s WHERE s.product = p)
            OR (SELECT s.availableQuantity FROM InventoryStock s WHERE s.product = p) <= 0
          )
    """)
    long countActiveProductsOutOfStock();
}
