package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findBySupplierCode(String supplierCode);

    boolean existsBySupplierCode(String supplierCode);

    @Query("SELECT s FROM Supplier s WHERE " +
           "(:active IS NULL OR s.isActive = :active) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(s.supplierName) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR " +
           "LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR " +
           "LOWER(s.contactPersonName) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))")
    Page<Supplier> searchSuppliers(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
