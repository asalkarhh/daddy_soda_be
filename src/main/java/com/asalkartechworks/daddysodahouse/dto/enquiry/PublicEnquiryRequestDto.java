package com.asalkartechworks.daddysodahouse.dto.enquiry;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public request DTO for prospective franchise inquiries submitted via the website.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Public franchise inquiry submission request")
public class PublicEnquiryRequestDto {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    @Schema(description = "Prospect applicant full name", example = "Amit Patil")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile must be a valid 10-digit Indian mobile number")
    @Schema(description = "10-digit mobile number", example = "9876543210")
    private String mobile;

    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Schema(description = "Applicant email address", example = "amit@example.com")
    private String email;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "Applicant city", example = "Pune")
    private String city;

    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    @Schema(description = "Enquiry inquiry details or location preferences", example = "I want details about soda franchise opportunity in Pune.")
    private String message;
}
