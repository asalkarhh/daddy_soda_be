package com.asalkartechworks.daddysodahouse.dto.iot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotEventIngestResponseDto {

    private String eventId;
    private boolean duplicate;
    private String machineCode;
    private Instant receivedAt;
}
