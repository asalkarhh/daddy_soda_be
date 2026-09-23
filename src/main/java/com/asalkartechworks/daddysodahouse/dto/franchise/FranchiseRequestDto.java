package com.asalkartechworks.daddysodahouse.dto.franchise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Franchise creation/update request payload")
public class FranchiseRequestDto {

    @NotBlank(message = "Franchise code is mandatory")
    @Size(max = 30, message = "Franchise code cannot exceed 30 characters")
    @Schema(description = "Unique alphanumeric franchise code", example = "FR-PUN-001")
    private String franchiseCode;

    @NotBlank(message = "Business name is mandatory")
    @Size(max = 150, message = "Business name cannot exceed 150 characters")
    @Schema(description = "Registered trade name of the franchise", example = "Daddy Soda Kothrud")
    private String businessName;

    @NotBlank(message = "Owner name is mandatory")
    @Size(max = 150, message = "Owner name cannot exceed 150 characters")
    @Schema(description = "Full name of the franchise owner", example = "Rahul Patil")
    private String ownerName;

    @NotBlank(message = "Mobile number is mandatory")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be a 10-digit number")
    @Schema(description = "10-digit primary mobile number", example = "9876543210")
    private String mobile;

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    @Schema(description = "Contact email address", example = "rahul.patil@example.com")
    private String email;

    @Pattern(regexp = "^$|^[0-9]{14}$", message = "FSSAI number must be 14 digits if provided")
    @Schema(description = "14-digit FSSAI license number (optional)", example = "11522001000123")
    private String fssaiNumber;

    @Schema(description = "Street address", example = "Shop 12, Mayur Colony, Kothrud")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Schema(description = "City", example = "Pune")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Pattern(regexp = "^$|^[0-9]{6}$", message = "Pincode must be 6 digits if provided")
    @Schema(description = "6-digit postal code", example = "411038")
    private String pincode;
}
