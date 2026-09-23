package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByItemCode(String itemCode);

    boolean existsByItemCode(String itemCode);

    @Query("SELECT p FROM Product p WHERE " +
           "(:active IS NULL OR p.isActive = :active) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.flavourName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.itemCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN InventoryStock s ON s.product.id = p.id WHERE " +
           "(:productId IS NULL OR p.id = :productId) AND " +
           "(:active IS NULL OR p.isActive = :active) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.flavourName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.itemCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:stockStatus IS NULL OR " +
           "(:stockStatus = 'IN_STOCK' AND COALESCE(s.availableQuantity, 0) > 0) OR " +
           "(:stockStatus = 'OUT_OF_STOCK' AND COALESCE(s.availableQuantity, 0) <= 0))")
    Page<Product> searchInventoryProducts(
            @Param("productId") UUID productId,
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("stockStatus") String stockStatus,
            Pageable pageable
    );
}
