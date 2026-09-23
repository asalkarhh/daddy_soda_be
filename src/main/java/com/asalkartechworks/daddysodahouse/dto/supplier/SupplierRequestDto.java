package com.asalkartechworks.daddysodahouse.dto.supplier;

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
@Schema(description = "Supplier creation/update request payload")
public class SupplierRequestDto {

    @NotBlank(message = "Supplier code is mandatory")
    @Size(max = 30, message = "Supplier code cannot exceed 30 characters")
    @Schema(description = "Unique supplier code", example = "SUP-BEV-001")
    private String supplierCode;

    @NotBlank(message = "Supplier name is mandatory")
    @Size(max = 150, message = "Supplier name cannot exceed 150 characters")
    @Schema(description = "Business name of the supplier", example = "Apex Flavours & Concentrates Pvt Ltd")
    private String supplierName;

    @Size(max = 150, message = "Contact person name cannot exceed 150 characters")
    @Schema(description = "Representative / contact person", example = "Vikram Deshmukh")
    private String contactPersonName;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Mobile must be 10 digits if provided")
    @Schema(description = "10-digit mobile number", example = "9823004455")
    private String mobile;

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    @Schema(description = "Contact email", example = "orders@apexflavours.com")
    private String email;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format (e.g. 27AAAAA0000A1Z5)")
    @Schema(description = "15-character GSTIN (optional)", example = "27ABCDE1234F1Z5")
    private String gstin;

    @Schema(description = "Supplier office/warehouse address", example = "Plot 45, Hadapsar Industrial Area, Pune")
    private String address;
}
