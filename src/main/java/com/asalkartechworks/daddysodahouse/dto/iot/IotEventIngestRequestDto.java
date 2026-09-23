package com.asalkartechworks.daddysodahouse.dto.iot;

import com.asalkartechworks.daddysodahouse.entity.IotEventStatus;
import com.asalkartechworks.daddysodahouse.entity.IotEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotEventIngestRequestDto {

    @NotBlank(message = "Event ID is mandatory")
    @Size(max = 100, message = "Event ID cannot exceed 100 characters")
    private String eventId;

    @NotBlank(message = "Machine ID is mandatory")
    @Size(max = 50, message = "Machine ID cannot exceed 50 characters")
    private String machineId;

    @NotBlank(message = "Device ID is mandatory")
    @Size(max = 100, message = "Device ID cannot exceed 100 characters")
    private String deviceId;

    @NotBlank(message = "Franchise ID is mandatory")
    @Size(max = 50, message = "Franchise ID cannot exceed 50 characters")
    private String franchiseId;

    @NotNull(message = "Event time is mandatory")
    private Instant eventTime;

    @NotNull(message = "Event type is mandatory")
    private IotEventType eventType;

    @Size(max = 150, message = "Drink name cannot exceed 150 characters")
    private String drinkName;

    @Valid
    private List<IotEventItemIngestRequestDto> items;

    @NotNull(message = "Status is mandatory")
    private IotEventStatus status;
}
