package com.asalkartechworks.daddysodahouse.dto.franchise;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Franchise details response")
public class FranchiseResponseDto {

    @Schema(description = "Franchise UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Unique franchise code", example = "FR-PUN-001")
    private String franchiseCode;

    @Schema(description = "Franchise business / trade name", example = "Daddy Soda Kothrud")
    private String businessName;

    @Schema(description = "Owner full name", example = "Rahul Patil")
    private String ownerName;

    @Schema(description = "Primary mobile number", example = "9876543210")
    private String mobile;

    @Schema(description = "Contact email", example = "rahul.patil@example.com")
    private String email;

    @Schema(description = "FSSAI license number", example = "11522001000123")
    private String fssaiNumber;

    @Schema(description = "Address line", example = "Shop 12, Mayur Colony, Kothrud")
    private String address;

    @Schema(description = "City", example = "Pune")
    private String city;

    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Schema(description = "Pincode", example = "411038")
    private String pincode;

    @Schema(description = "Whether franchise is active", example = "true")
    private boolean active;
    private BigDecimal availableCredit;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
