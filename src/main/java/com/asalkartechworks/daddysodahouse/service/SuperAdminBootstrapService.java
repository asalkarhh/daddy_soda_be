package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Startup service to bootstrap the initial SUPER_ADMIN account if none exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrapService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.name:}")
    private String adminName;

    @Value("${app.bootstrap.admin.mobile:}")
    private String adminMobile;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminMobile) || !StringUtils.hasText(adminPassword) || !StringUtils.hasText(adminName)) {
            log.info("Initial SUPER_ADMIN bootstrap skipped: environment variables (INITIAL_ADMIN_NAME, INITIAL_ADMIN_MOBILE, INITIAL_ADMIN_PASSWORD) not fully set.");
            return;
        }

        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            log.info("A SUPER_ADMIN account already exists. Skipping bootstrap.");
            return;
        }

        if (userRepository.existsByMobile(adminMobile)) {
            log.warn("User with mobile {} already exists but is not SUPER_ADMIN. Skipping bootstrap.", adminMobile);
            return;
        }

        User superAdmin = User.builder()
                .fullName(adminName.trim())
                .mobile(adminMobile.trim())
                .email(StringUtils.hasText(adminEmail) ? adminEmail.trim() : null)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build();

        userRepository.save(superAdmin);
        log.info("Initial SUPER_ADMIN account successfully bootstrapped for mobile: {}", adminMobile);
    }
}
