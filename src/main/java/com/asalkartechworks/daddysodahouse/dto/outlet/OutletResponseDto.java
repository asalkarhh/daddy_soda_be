package com.asalkartechworks.daddysodahouse.dto.outlet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Outlet details response")
public class OutletResponseDto {

    @Schema(description = "Outlet UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Parent franchise UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID franchiseId;

    @Schema(description = "Parent franchise business name", example = "Daddy Soda Kothrud")
    private String franchiseName;

    @Schema(description = "Unique outlet code", example = "OUT-KOT-01")
    private String outletCode;

    @Schema(description = "Name of the outlet", example = "Kothrud Depot Outlet")
    private String outletName;

    @Schema(description = "Contact person", example = "Suresh Shinde")
    private String contactPersonName;

    @Schema(description = "Contact mobile", example = "9822001122")
    private String contactMobile;

    @Schema(description = "Street address", example = "Near Kothrud Bus Stand, Karve Road")
    private String address;

    @Schema(description = "City", example = "Pune")
    private String city;

    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @Schema(description = "Pincode", example = "411038")
    private String pincode;

    @Schema(description = "Whether outlet is active", example = "true")
    private boolean active;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
