package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.StatusResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public health & status controller.
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Application Status", description = "Public endpoints for checking application health and readiness")
public class StatusController {

    private static final String APP_NAME = "Daddy Soda House Backend";
    private static final String STATUS_UP = "UP";
    private static final String SUCCESS_MESSAGE = "Daddy Soda House API is running";

    @GetMapping("/status")
    @Operation(summary = "Check API status", description = "Returns the operational status of Daddy Soda House API without authentication")
    public ResponseEntity<ApiResponse<StatusResponseDto>> getStatus() {
        StatusResponseDto statusData = StatusResponseDto.builder()
                .application(APP_NAME)
                .status(STATUS_UP)
                .build();

        return ResponseEntity.ok(ApiResponse.success(statusData, SUCCESS_MESSAGE));
    }
}
