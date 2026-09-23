package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.auth.UserSummaryDto;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for user retrieval and state management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public User findByMobile(String mobile) {
        return userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("User", "mobile", mobile));
    }

    @Transactional
    public void updateLastLoginAt(UUID userId) {
        User user = findById(userId);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }

    public UserSummaryDto toSummaryDto(User user) {
        UUID franchiseId = user.getFranchise() != null ? user.getFranchise().getId() : null;
        String franchiseName = user.getFranchise() != null ? user.getFranchise().getBusinessName() : null;

        return UserSummaryDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .role(user.getRole())
                .franchiseId(franchiseId)
                .franchiseName(franchiseName)
                .active(user.isActive())
                .build();
    }
}
