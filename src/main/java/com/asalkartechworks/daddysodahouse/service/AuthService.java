package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.auth.LoginRequestDto;
import com.asalkartechworks.daddysodahouse.dto.auth.LoginResponseDto;
import com.asalkartechworks.daddysodahouse.dto.auth.UserSummaryDto;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.exception.AuthException;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import com.asalkartechworks.daddysodahouse.security.JwtService;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service orchestrating authentication flows: login, user identity retrieval, and logout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    /**
     * Authenticate user credentials, enforce active status, and issue a JWT token.
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByMobile(request.getMobile())
                .orElseThrow(() -> new AuthException("Invalid mobile or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Invalid mobile or password");
        }

        if (!user.isActive()) {
            throw new AuthException("User account is inactive. Please contact administrator.");
        }

        // Update last login timestamp
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtService.generateToken(principal);

        return LoginResponseDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(userService.toSummaryDto(user))
                .build();
    }

    /**
     * Retrieve the currently authenticated user's profile summary.
     */
    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UserPrincipal principal) {
        User user = userService.findById(principal.getId());
        return userService.toSummaryDto(user);
    }

    /**
     * Stateless logout acknowledgment.
     */
    public String logout() {
        return "Logged out successfully. Please remove your access token from client storage.";
    }
}
