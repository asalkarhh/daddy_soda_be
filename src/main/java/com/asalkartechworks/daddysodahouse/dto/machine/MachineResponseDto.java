package com.asalkartechworks.daddysodahouse.dto.machine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Machine details response")
public class MachineResponseDto {

    @Schema(description = "Machine UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Outlet UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID outletId;

    @Schema(description = "Outlet name", example = "Kothrud Depot Outlet")
    private String outletName;

    @Schema(description = "Franchise UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID franchiseId;

    @Schema(description = "Franchise business name", example = "Daddy Soda Kothrud")
    private String franchiseName;

    @Schema(description = "Unique machine code", example = "MAC-KOT-01")
    private String machineCode;

    @Schema(description = "Machine model / display name", example = "8-Valve Soda Fountain Model X")
    private String machineName;

    @Schema(description = "IoT device hardware identifier", example = "DSH-IOT-9901")
    private String deviceId;

    @Schema(description = "Installation date", example = "2026-05-15")
    private LocalDate installationDate;

    @Schema(description = "Last telemetry / ping timestamp")
    private Instant lastSeenAt;

    @Schema(description = "Whether machine is active", example = "true")
    private boolean active;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
