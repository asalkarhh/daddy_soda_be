package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.Outlet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutletRepository extends JpaRepository<Outlet, UUID> {

    Optional<Outlet> findByOutletCode(String outletCode);

    boolean existsByOutletCode(String outletCode);

    boolean existsByFranchiseIdAndIsActiveTrue(UUID franchiseId);

    List<Outlet> findByFranchiseIdAndIsActiveTrue(UUID franchiseId);

    @Query("SELECT o FROM Outlet o WHERE " +
           "(:franchiseId IS NULL OR o.franchise.id = :franchiseId) AND " +
           "(:active IS NULL OR o.isActive = :active) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(o.outletName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.outletCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Outlet> searchOutlets(
            @Param("franchiseId") UUID franchiseId,
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
