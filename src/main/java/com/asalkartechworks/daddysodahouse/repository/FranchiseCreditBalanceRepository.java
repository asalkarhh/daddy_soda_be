package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.FranchiseCreditBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FranchiseCreditBalanceRepository extends JpaRepository<FranchiseCreditBalance, UUID> {
    Optional<FranchiseCreditBalance> findByFranchiseId(UUID franchiseId);
}
