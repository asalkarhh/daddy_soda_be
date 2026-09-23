package com.asalkartechworks.daddysodahouse.repository;

import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User persistence operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    Optional<User> findByEmail(String email);

    boolean existsByRole(UserRole role);

    long countByRoleAndIsActiveTrue(UserRole role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:franchiseId IS NULL OR u.franchise.id = :franchiseId) AND " +
           "(:active IS NULL OR u.isActive = :active)")
    org.springframework.data.domain.Page<User> searchUsers(
            @org.springframework.data.repository.query.Param("role") UserRole role,
            @org.springframework.data.repository.query.Param("franchiseId") UUID franchiseId,
            @org.springframework.data.repository.query.Param("active") Boolean active,
            org.springframework.data.domain.Pageable pageable
    );
}
