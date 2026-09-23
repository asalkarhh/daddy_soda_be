package com.asalkartechworks.daddysodahouse.dto.user;

import com.asalkartechworks.daddysodahouse.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User update request payload")
public class UserUpdateRequestDto {

    @NotBlank(message = "Full name is mandatory")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    @Schema(description = "Full name of the user", example = "Rahul S. Patil")
    private String fullName;

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    @Schema(description = "Email address (optional)", example = "rahul.patil@example.com")
    private String email;

    @NotNull(message = "User role is mandatory")
    @Schema(description = "Role: SUPER_ADMIN, MANAGER, or FRANCHISE_OWNER", example = "FRANCHISE_OWNER")
    private UserRole role;

    @Schema(description = "Associated franchise UUID (mandatory for FRANCHISE_OWNER, null for others)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID franchiseId;

    @NotNull(message = "Active status is mandatory")
    @Schema(description = "Active status flag", example = "true")
    private Boolean active;
}
