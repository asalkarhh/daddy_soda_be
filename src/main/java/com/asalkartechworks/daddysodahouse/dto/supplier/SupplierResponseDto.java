package com.asalkartechworks.daddysodahouse.dto.supplier;

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
@Schema(description = "Supplier details response")
public class SupplierResponseDto {

    @Schema(description = "Supplier UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Unique supplier code", example = "SUP-BEV-001")
    private String supplierCode;

    @Schema(description = "Supplier business name", example = "Apex Flavours & Concentrates Pvt Ltd")
    private String supplierName;

    @Schema(description = "Contact person", example = "Vikram Deshmukh")
    private String contactPersonName;

    @Schema(description = "Mobile number", example = "9823004455")
    private String mobile;

    @Schema(description = "Contact email", example = "orders@apexflavours.com")
    private String email;

    @Schema(description = "15-character GSTIN", example = "27ABCDE1234F1Z5")
    private String gstin;

    @Schema(description = "Address line", example = "Plot 45, Hadapsar Industrial Area, Pune")
    private String address;

    @Schema(description = "Whether supplier is active", example = "true")
    private boolean active;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
