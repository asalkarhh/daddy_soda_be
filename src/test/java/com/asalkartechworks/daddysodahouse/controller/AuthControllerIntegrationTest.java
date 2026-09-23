package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.auth.LoginRequestDto;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthControllerIntegrationTest(MockMvc mockMvc,
                                         UserRepository userRepository,
                                         PasswordEncoder passwordEncoder,
                                         ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Seed an active SUPER_ADMIN user
        User superAdmin = User.builder()
                .fullName("Super Admin")
                .mobile("9876543210")
                .email("superadmin@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build();
        userRepository.save(superAdmin);

        // Seed an inactive MANAGER user
        User inactiveUser = User.builder()
                .fullName("Inactive Manager")
                .mobile("9123456780")
                .email("inactive@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Manager@12345"))
                .role(UserRole.MANAGER)
                .isActive(false)
                .build();
        userRepository.save(inactiveUser);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login succeeds with valid credentials and returns JWT")
    void login_withValidCredentials_returns200AndJwtToken() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .mobile("9876543210")
                .password("Admin@12345")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andExpect(jsonPath("$.data.user.fullName").value("Super Admin"))
                .andExpect(jsonPath("$.data.user.mobile").value("9876543210"))
                .andExpect(jsonPath("$.data.user.email").value("superadmin@daddysoda.com"))
                .andExpect(jsonPath("$.data.user.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.user.active").value(true))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();

        // Verify last_login_at was updated
        User updatedUser = userRepository.findByMobile("9876543210").orElseThrow();
        assertThat(updatedUser.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login fails with invalid password")
    void login_withInvalidPassword_returns401() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .mobile("9876543210")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid mobile or password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login fails when user does not exist")
    void login_withNonExistentMobile_returns401() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .mobile("9000000000")
                .password("AnyPassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid mobile or password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login fails for inactive user")
    void login_withInactiveUser_returns401() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .mobile("9123456780")
                .password("Manager@12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User account is inactive. Please contact administrator."));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login fails with invalid mobile format")
    void login_withInvalidMobileFormat_returns400() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .mobile("123") // Not 10 digits
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.mobile").exists());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me succeeds with valid JWT")
    void getCurrentUser_withValidJwt_returns200() throws Exception {
        String token = obtainAccessToken("9876543210", "Admin@12345");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current user retrieved successfully"))
                .andExpect(jsonPath("$.data.fullName").value("Super Admin"))
                .andExpect(jsonPath("$.data.mobile").value("9876543210"))
                .andExpect(jsonPath("$.data.email").value("superadmin@daddysoda.com"))
                .andExpect(jsonPath("$.data.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me fails without JWT token")
    void getCurrentUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me fails with invalid/tampered JWT token")
    void getCurrentUser_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.token.payload"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired JWT token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout succeeds with valid JWT")
    void logout_withValidToken_returns200() throws Exception {
        String token = obtainAccessToken("9876543210", "Admin@12345");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully. Please remove your access token from client storage."));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout fails without token")
    void logout_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    private String obtainAccessToken(String mobile, String password) throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .mobile(mobile)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}
