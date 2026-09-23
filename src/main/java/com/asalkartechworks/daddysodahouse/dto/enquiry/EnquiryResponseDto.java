package com.asalkartechworks.daddysodahouse.dto.enquiry;

import com.asalkartechworks.daddysodahouse.entity.EnquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed enquiry response DTO for administrative review.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Full enquiry response details")
public class EnquiryResponseDto {

    private UUID id;
    private String enquiryNumber;
    private String fullName;
    private String mobile;
    private String email;
    private String city;
    private String message;
    private String source;
    private EnquiryStatus status;
    private String adminNotes;
    private Instant contactedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private String whatsAppUrl;
}
