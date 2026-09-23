package com.asalkartechworks.daddysodahouse.security;

import com.asalkartechworks.daddysodahouse.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "super_secure_test_jwt_secret_key_minimum_256_bits_length_1234567890";
    private static final long EXPIRATION_MS = 60000; // 1 minute

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("JWT generation and validation succeed with claims for userId, mobile, and role")
    void generateToken_containsRequiredClaimsAndValidatesSuccessfully() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .mobile("9876543210")
                .fullName("Test Super Admin")
                .email("admin@test.com")
                .role(UserRole.SUPER_ADMIN)
                .active(true)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .build();

        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractMobile(token)).isEqualTo("9876543210");
        assertThat(jwtService.extractRole(token)).isEqualTo("SUPER_ADMIN");
    }

    @Test
    @DisplayName("Validation fails when token is expired")
    void validateToken_expiredTokenReturnsFalse() throws InterruptedException {
        // Service with 1ms expiration
        JwtService shortLivedJwtService = new JwtService(TEST_SECRET, 1);

        UserPrincipal principal = UserPrincipal.builder()
                .id(UUID.randomUUID())
                .mobile("9123456789")
                .role(UserRole.MANAGER)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))
                .build();

        String token = shortLivedJwtService.generateToken(principal);

        // Wait for token to expire
        Thread.sleep(10);

        assertThat(shortLivedJwtService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Validation fails for malformed token")
    void validateToken_malformedTokenReturnsFalse() {
        assertThat(jwtService.validateToken("invalid.malformed.token")).isFalse();
    }
}
