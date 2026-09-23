package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventDetailResponseDto;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.iot.MachineStatusResponseDto;
import com.asalkartechworks.daddysodahouse.entity.IotEventStatus;
import com.asalkartechworks.daddysodahouse.entity.IotEventType;
import com.asalkartechworks.daddysodahouse.service.IotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller for administrators and managers to inspect IoT machine telemetry and connectivity.
 */
@RestController
@RequestMapping("/api/v1/iot")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "IoT Administration", description = "Endpoints for Super Admin and Manager to view IoT events and machine connectivity")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
public class IotAdminController {

    private final IotService iotService;

    @Operation(summary = "Search IoT Telemetry Events", description = "Returns a paginated list of IoT event summaries with multiple filters")
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<PageResponse<IotEventSummaryDto>>> searchEvents(
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) IotEventType eventType,
            @RequestParam(required = false) IotEventStatus eventStatus,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDateTime,
            @PageableDefault(size = 20, sort = "eventTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<IotEventSummaryDto> result = iotService.searchEvents(
                franchiseId, machineId, deviceId, eventType, eventStatus, productId, fromDateTime, toDateTime, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(result, "IoT events retrieved successfully"));
    }

    @Operation(summary = "Get IoT Event Details", description = "Retrieves complete IoT event record including items and raw JSON payload")
    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<IotEventDetailResponseDto>> getEventById(@PathVariable UUID id) {
        IotEventDetailResponseDto result = iotService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success(result, "IoT event details retrieved successfully"));
    }

    @Operation(summary = "Get Machine Connectivity Status", description = "Lists all active soda machines with ONLINE / OFFLINE / NEVER_CONNECTED status")
    @GetMapping("/machines/status")
    public ResponseEntity<ApiResponse<List<MachineStatusResponseDto>>> getMachineStatuses() {
        List<MachineStatusResponseDto> result = iotService.getMachineStatuses();
        return ResponseEntity.ok(ApiResponse.success(result, "Machine connection statuses retrieved successfully"));
    }
}
