package com.asalkartechworks.daddysodahouse.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User login credentials payload")
public class LoginRequestDto {

    @NotBlank(message = "Mobile number is mandatory")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    @Schema(description = "10-digit registered mobile number", example = "9876543210")
    private String mobile;

    @NotBlank(message = "Password is mandatory")
    @Schema(description = "Account password", example = "Admin@12345")
    private String password;
}
