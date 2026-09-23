package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventIngestRequestDto;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventIngestResponseDto;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.service.IotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Public machine-to-server endpoint for IoT telemetry ingestion.
 * Authenticated strictly using header X-API-KEY (no JWT).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/iot")
@RequiredArgsConstructor
@Tag(name = "IoT Telemetry Ingestion", description = "Machine-to-server HTTPS API for dispensing and heartbeat events")
public class IotIngestController {

    private final IotService iotService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Operation(
            summary = "Ingest IoT Machine Telemetry Event",
            description = "Receives drink dispense or heartbeat telemetry from an IoT soda machine. " +
                          "Requires secret header X-API-KEY. Machine must communicate over HTTPS in production."
    )
    @PostMapping("/events")
    public ResponseEntity<ApiResponse<IotEventIngestResponseDto>> ingestEvent(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody String rawPayload
    ) {
        // 1. Authenticate API Key
        iotService.validateApiKey(apiKey);

        // 2. Deserialize JSON payload
        IotEventIngestRequestDto requestDto;
        try {
            requestDto = objectMapper.readValue(rawPayload, IotEventIngestRequestDto.class);
        } catch (Exception e) {
            log.warn("Malformed JSON received on IoT ingestion endpoint: {}", e.getMessage());
            throw new BadRequestException("Malformed JSON payload: " + e.getMessage());
        }

        // 3. Validate Bean Constraints
        Set<ConstraintViolation<IotEventIngestRequestDto>> violations = validator.validate(requestDto);
        if (!violations.isEmpty()) {
            String validationMsg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .findFirst()
                    .orElse("Validation failed");
            throw new BadRequestException(validationMsg);
        }

        // 4. Ingest and persist event
        IotEventIngestResponseDto responseDto = iotService.ingestEvent(requestDto, rawPayload);

        String message = responseDto.isDuplicate()
                ? "Duplicate event already processed"
                : "IoT event processed successfully";

        return ResponseEntity.ok(ApiResponse.success(responseDto, message));
    }
}
