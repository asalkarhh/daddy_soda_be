package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.Machine;
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
public interface MachineRepository extends JpaRepository<Machine, UUID> {

    Optional<Machine> findByMachineCode(String machineCode);

    boolean existsByMachineCode(String machineCode);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndIdNot(String deviceId, UUID id);

    boolean existsByOutletIdAndIsActiveTrue(UUID outletId);

    List<Machine> findByOutletFranchiseIdAndIsActiveTrue(UUID franchiseId);

    long countByOutletFranchiseId(UUID franchiseId);

    long countByOutletFranchiseIdAndIsActiveTrue(UUID franchiseId);

    long countByIsActiveTrue();

    List<Machine> findAllByIsActiveTrueOrderByMachineCodeAsc();

    @Query("SELECT m FROM Machine m WHERE " +
           "(:outletId IS NULL OR m.outlet.id = :outletId) AND " +
           "(:franchiseId IS NULL OR m.outlet.franchise.id = :franchiseId) AND " +
           "(:active IS NULL OR m.isActive = :active) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(m.machineName) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR " +
           "LOWER(m.machineCode) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR " +
           "LOWER(m.deviceId) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))")
    Page<Machine> searchMachines(
            @Param("outletId") UUID outletId,
            @Param("franchiseId") UUID franchiseId,
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
