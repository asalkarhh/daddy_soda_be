package com.asalkartechworks.daddysodahouse.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration enforcing stateless JWT authentication,
 * CORS, public routes, and error handling.
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final Environment environment;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        List<String> defaultProfiles = Arrays.asList(environment.getDefaultProfiles());
        boolean isDevProfile = activeProfiles.contains("dev")
                || (activeProfiles.isEmpty() && defaultProfiles.contains("dev"));

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> {
                // Actuator health check endpoint
                auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();

                // Swagger / OpenAPI documentation endpoints (allowed only in dev profile)
                if (isDevProfile) {
                    auth.requestMatchers(
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/swagger-ui/**"
                    ).permitAll();
                }

                // Public API routes
                auth.requestMatchers("/api/v1/public/**").permitAll();

                // Auth public endpoints
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll();

                // Auth authenticated endpoints
                auth.requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout").authenticated();

                // All other /api/v1/** endpoints require authentication by default
                auth.requestMatchers("/api/v1/**").authenticated();

                // Any other endpoint requires authentication
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
