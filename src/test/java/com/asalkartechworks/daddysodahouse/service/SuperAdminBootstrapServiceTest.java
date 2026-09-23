package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SuperAdminBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapService = new SuperAdminBootstrapService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Bootstraps SUPER_ADMIN when none exists and environment variables are present")
    void run_createsSuperAdminWhenNoneExists() {
        ReflectionTestUtils.setField(bootstrapService, "adminName", "Initial Admin");
        ReflectionTestUtils.setField(bootstrapService, "adminMobile", "9999999999");
        ReflectionTestUtils.setField(bootstrapService, "adminEmail", "admin@daddysoda.com");
        ReflectionTestUtils.setField(bootstrapService, "adminPassword", "Secure@12345");

        when(userRepository.existsByRole(UserRole.SUPER_ADMIN)).thenReturn(false);
        when(userRepository.existsByMobile("9999999999")).thenReturn(false);
        when(passwordEncoder.encode("Secure@12345")).thenReturn("hashed_password");

        bootstrapService.run(new DefaultApplicationArguments());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User created = captor.getValue();
        assertThat(created.getFullName()).isEqualTo("Initial Admin");
        assertThat(created.getMobile()).isEqualTo("9999999999");
        assertThat(created.getEmail()).isEqualTo("admin@daddysoda.com");
        assertThat(created.getPasswordHash()).isEqualTo("hashed_password");
        assertThat(created.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("Skips bootstrap when a SUPER_ADMIN already exists")
    void run_skipsWhenSuperAdminAlreadyExists() {
        ReflectionTestUtils.setField(bootstrapService, "adminName", "Initial Admin");
        ReflectionTestUtils.setField(bootstrapService, "adminMobile", "9999999999");
        ReflectionTestUtils.setField(bootstrapService, "adminPassword", "Secure@12345");

        when(userRepository.existsByRole(UserRole.SUPER_ADMIN)).thenReturn(true);

        bootstrapService.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Skips bootstrap safely when required environment variables are absent")
    void run_skipsWhenEnvironmentVariablesAbsent() {
        ReflectionTestUtils.setField(bootstrapService, "adminName", "");
        ReflectionTestUtils.setField(bootstrapService, "adminMobile", "");
        ReflectionTestUtils.setField(bootstrapService, "adminPassword", "");

        bootstrapService.run(new DefaultApplicationArguments());

        verify(userRepository, never()).existsByRole(any());
        verify(userRepository, never()).save(any(User.class));
    }
}
