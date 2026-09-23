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
 * Summary DTO for paginated enquiry management listings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Enquiry summary item")
public class EnquirySummaryDto {

    private UUID id;
    private String enquiryNumber;
    private String fullName;
    private String mobile;
    private String email;
    private String city;
    private EnquiryStatus status;
    private Instant contactedAt;
    private Instant createdAt;
}
