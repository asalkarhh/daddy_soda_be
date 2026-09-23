package com.asalkartechworks.daddysodahouse.dto.outlet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Outlet creation/update request payload")
public class OutletRequestDto {

    @NotNull(message = "Franchise ID is mandatory")
    @Schema(description = "UUID of parent franchise", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID franchiseId;

    @NotBlank(message = "Outlet code is mandatory")
    @Size(max = 30, message = "Outlet code cannot exceed 30 characters")
    @Schema(description = "Unique outlet identifier code", example = "OUT-KOT-01")
    private String outletCode;

    @NotBlank(message = "Outlet name is mandatory")
    @Size(max = 150, message = "Outlet name cannot exceed 150 characters")
    @Schema(description = "Name of the outlet location", example = "Kothrud Depot Outlet")
    private String outletName;

    @Size(max = 150, message = "Contact person name cannot exceed 150 characters")
    @Schema(description = "Outlet manager/contact person", example = "Suresh Shinde")
    private String contactPersonName;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Contact mobile must be 10 digits if provided")
    @Schema(description = "10-digit mobile number", example = "9822001122")
    private String contactMobile;

    @Schema(description = "Outlet street address", example = "Near Kothrud Bus Stand, Karve Road")
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
