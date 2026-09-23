package com.asalkartechworks.daddysodahouse.dto.auth;

import com.asalkartechworks.daddysodahouse.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Safe user details representation")
public class UserSummaryDto {

    @Schema(description = "Unique user identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Full name of the user", example = "Admin Name")
    private String fullName;

    @Schema(description = "Registered 10-digit mobile number", example = "9876543210")
    private String mobile;

    @Schema(description = "Registered email address", example = "admin@example.com")
    private String email;

    @Schema(description = "Assigned user role", example = "SUPER_ADMIN")
    private UserRole role;

    @Schema(description = "Associated franchise UUID (for FRANCHISE_OWNER)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID franchiseId;

    @Schema(description = "Associated franchise name (for FRANCHISE_OWNER)", example = "Daddy Soda Kothrud")
    private String franchiseName;

    @Schema(description = "Whether the user account is active", example = "true")
    private boolean active;
}
