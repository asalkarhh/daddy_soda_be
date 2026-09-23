package com.asalkartechworks.daddysodahouse.dto.iot;

import com.asalkartechworks.daddysodahouse.entity.IotEventStatus;
import com.asalkartechworks.daddysodahouse.entity.IotEventType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotEventDetailResponseDto {

    private UUID id;
    private String eventId;
    private UUID machineId;
    private String machineCode;
    private String machineName;
    private UUID franchiseId;
    private String franchiseName;
    private Instant eventTime;
    private IotEventType eventType;
    private String drinkName;
    private BigDecimal syrupQuantityMl;
    private BigDecimal rawSodaQuantityMl;
    private IotEventStatus eventStatus;
    private Instant receivedAt;
    private Instant createdAt;
    private List<IotEventItemResponseDto> items;
    private JsonNode rawPayload;
}
