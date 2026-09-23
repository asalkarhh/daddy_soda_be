package com.asalkartechworks.daddysodahouse.dto.iot;

import com.asalkartechworks.daddysodahouse.entity.IotEventStatus;
import com.asalkartechworks.daddysodahouse.entity.IotEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotEventSummaryDto {

    private UUID id;
    private String eventId;
    private String machineCode;
    private String machineName;
    private String franchiseName;
    private Instant eventTime;
    private IotEventType eventType;
    private String drinkName;
    private BigDecimal syrupQuantityMl;
    private BigDecimal rawSodaQuantityMl;
    private IotEventStatus eventStatus;
    private Instant receivedAt;
}
