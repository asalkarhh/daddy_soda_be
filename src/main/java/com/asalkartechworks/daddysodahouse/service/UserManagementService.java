package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.auth.UserSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.user.UserCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.user.UserUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final FranchiseRepository franchiseRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Transactional
    public UserSummaryDto createUser(UserCreateRequestDto dto) {
        if (dto.getRole() == UserRole.SUPER_ADMIN) {
            throw new BadRequestException("Creating SUPER_ADMIN through API is not permitted");
        }

        if (userRepository.existsByMobile(dto.getMobile().trim())) {
            throw new BadRequestException("Mobile number already in use: " + dto.getMobile());
        }

        if (StringUtils.hasText(dto.getEmail()) && userRepository.findByEmail(dto.getEmail().trim()).isPresent()) {
            throw new BadRequestException("Email address already in use: " + dto.getEmail());
        }

        Franchise franchise = null;
        if (dto.getRole() == UserRole.FRANCHISE_OWNER) {
            if (dto.getFranchiseId() == null) {
                throw new BadRequestException("Franchise ID is mandatory for FRANCHISE_OWNER");
            }
            franchise = franchiseRepository.findById(dto.getFranchiseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", dto.getFranchiseId()));
            if (!franchise.isActive()) {
                throw new BadRequestException("Cannot assign user to an inactive franchise");
            }
        } else if (dto.getRole() == UserRole.MANAGER) {
            if (dto.getFranchiseId() != null) {
                throw new BadRequestException("Franchise ID must not be assigned to MANAGER role");
            }
        }

        User user = User.builder()
                .fullName(dto.getFullName().trim())
                .mobile(dto.getMobile().trim())
                .email(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .franchise(franchise)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Created user ID: {} with role: {}", saved.getId(), saved.getRole());
        return userService.toSummaryDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryDto> getAllUsers(UserRole role, UUID franchiseId, Boolean active, Pageable pageable) {
        Page<User> page = userRepository.searchUsers(role, franchiseId, active, pageable);
        return PageResponse.from(page.map(userService::toSummaryDto));
    }

    @Transactional
    public UserSummaryDto updateUser(UUID id, UserUpdateRequestDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Check last active SUPER_ADMIN protection
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            if (dto.getRole() != UserRole.SUPER_ADMIN && userRepository.countByRoleAndIsActiveTrue(UserRole.SUPER_ADMIN) <= 1) {
                throw new BadRequestException("Cannot change role of the last active SUPER_ADMIN");
            }
            if (!dto.getActive() && userRepository.countByRoleAndIsActiveTrue(UserRole.SUPER_ADMIN) <= 1) {
                throw new BadRequestException("Cannot deactivate the last active SUPER_ADMIN");
            }
        }

        // Validate email uniqueness if changed
        if (StringUtils.hasText(dto.getEmail())) {
            String newEmail = dto.getEmail().trim();
            userRepository.findByEmail(newEmail)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new BadRequestException("Email address already in use: " + newEmail);
                    });
            user.setEmail(newEmail);
        } else {
            user.setEmail(null);
        }

        Franchise franchise = null;
        if (dto.getRole() == UserRole.FRANCHISE_OWNER) {
            if (dto.getFranchiseId() == null) {
                throw new BadRequestException("Franchise ID is mandatory for FRANCHISE_OWNER");
            }
            franchise = franchiseRepository.findById(dto.getFranchiseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", dto.getFranchiseId()));
            if (!franchise.isActive()) {
                throw new BadRequestException("Cannot assign user to an inactive franchise");
            }
        } else if (dto.getRole() == UserRole.MANAGER || dto.getRole() == UserRole.SUPER_ADMIN) {
            if (dto.getFranchiseId() != null) {
                throw new BadRequestException("Franchise ID must not be assigned to " + dto.getRole() + " role");
            }
        }

        user.setFullName(dto.getFullName().trim());
        user.setRole(dto.getRole());
        user.setFranchise(franchise);
        user.setActive(dto.getActive());

        User updated = userRepository.save(user);
        log.info("Updated user ID: {}", updated.getId());
        return userService.toSummaryDto(updated);
    }

    @Transactional
    public UserSummaryDto updateStatus(UUID id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!active && user.getRole() == UserRole.SUPER_ADMIN
                && userRepository.countByRoleAndIsActiveTrue(UserRole.SUPER_ADMIN) <= 1) {
            throw new BadRequestException("Cannot deactivate the last active SUPER_ADMIN");
        }

        user.setActive(active);
        User updated = userRepository.save(user);
        log.info("User ID: {} status updated to active={}", updated.getId(), active);
        return userService.toSummaryDto(updated);
    }
}
