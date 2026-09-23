package com.asalkartechworks.daddysodahouse.dto.machine;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Machine creation/update request payload")
public class MachineRequestDto {

    @NotNull(message = "Outlet ID is mandatory")
    @Schema(description = "UUID of target outlet", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID outletId;

    @NotBlank(message = "Machine code is mandatory")
    @Size(max = 50, message = "Machine code cannot exceed 50 characters")
    @Schema(description = "Unique machine identifier code", example = "MAC-KOT-01")
    private String machineCode;

    @Size(max = 150, message = "Machine name cannot exceed 150 characters")
    @Schema(description = "Display name / model description", example = "8-Valve Soda Fountain Model X")
    private String machineName;

    @Size(max = 100, message = "Device ID cannot exceed 100 characters")
    @Schema(description = "Optional unique hardware IoT device identifier", example = "DSH-IOT-9901")
    private String deviceId;

    @Schema(description = "Equipment installation date (YYYY-MM-DD)", example = "2026-05-15")
    private LocalDate installationDate;
}
