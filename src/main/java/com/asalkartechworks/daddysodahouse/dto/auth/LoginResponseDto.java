package com.asalkartechworks.daddysodahouse.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response payload containing JWT access token and user info")
public class LoginResponseDto {

    @Schema(description = "JWT Bearer access token", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String accessToken;

    @Builder.Default
    @Schema(description = "Token type scheme", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Token expiration duration in milliseconds", example = "86400000")
    private long expiresIn;

    @Schema(description = "Authenticated user profile summary")
    private UserSummaryDto user;
}
