package com.asalkartechworks.daddysodahouse.dto.iot;

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
public class MachineStatusResponseDto {

    private UUID machineId;
    private String machineCode;
    private String machineName;
    private String franchiseName;
    private String deviceId;
    private Instant lastSeenAt;
    private MachineConnectionStatus connectionStatus;
    private Long minutesSinceLastSeen;
}
