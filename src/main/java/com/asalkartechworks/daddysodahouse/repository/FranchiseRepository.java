package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.Franchise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FranchiseRepository extends JpaRepository<Franchise, UUID> {

    Optional<Franchise> findByFranchiseCode(String franchiseCode);

    boolean existsByFranchiseCode(String franchiseCode);

    long countByIsActiveTrue();

    @Query("SELECT f FROM Franchise f WHERE " +
           "(:active IS NULL OR f.isActive = :active) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(f.businessName) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR " +
           "LOWER(f.ownerName) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR " +
           "LOWER(f.franchiseCode) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR " +
           "LOWER(f.city) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))")
    Page<Franchise> searchFranchises(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
