package com.asalkartechworks.daddysodahouse.dto.enquiry;

import com.asalkartechworks.daddysodahouse.entity.EnquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an enquiry status and appending admin notes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Enquiry status update request")
public class EnquiryStatusUpdateRequestDto {

    @NotNull(message = "Enquiry status is required")
    @Schema(description = "New enquiry status", example = "CONTACTED")
    private EnquiryStatus status;

    @Size(max = 1000, message = "Admin notes must not exceed 1000 characters")
    @Schema(description = "Operational follow-up notes", example = "Called and shared franchise brochure.")
    private String adminNotes;
}
