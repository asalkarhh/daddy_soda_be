package com.asalkartechworks.daddysodahouse.dto.user;

import com.asalkartechworks.daddysodahouse.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "User creation request payload (by Super Admin)")
public class UserCreateRequestDto {

    @NotBlank(message = "Full name is mandatory")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    @Schema(description = "Full name of the user", example = "Rahul Patil")
    private String fullName;

    @NotBlank(message = "Mobile number is mandatory")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    @Schema(description = "10-digit primary mobile number", example = "9876543210")
    private String mobile;

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    @Schema(description = "Email address (optional)", example = "rahul@example.com")
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Temporary password for the user", example = "TemporaryPassword@123")
    private String password;

    @NotNull(message = "User role is mandatory")
    @Schema(description = "Role: MANAGER or FRANCHISE_OWNER", example = "FRANCHISE_OWNER")
    private UserRole role;

    @Schema(description = "Associated franchise UUID (mandatory for FRANCHISE_OWNER, must be null for MANAGER)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID franchiseId;
}
