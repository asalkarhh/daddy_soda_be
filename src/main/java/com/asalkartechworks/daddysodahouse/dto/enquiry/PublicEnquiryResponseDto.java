package com.asalkartechworks.daddysodahouse.dto.enquiry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Public response DTO returned after website inquiry submission containing the WhatsApp click-to-chat URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Public enquiry creation response containing WhatsApp redirect link")
public class PublicEnquiryResponseDto {

    @Schema(description = "Generated enquiry ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID enquiryId;

    @Schema(description = "Generated unique enquiry tracking number", example = "ENQ-202609-00001")
    private String enquiryNumber;

    @Schema(description = "Prefilled WhatsApp click-to-chat redirect URL for client browser navigation", example = "https://wa.me/919876543210?text=...")
    private String whatsAppUrl;
}
