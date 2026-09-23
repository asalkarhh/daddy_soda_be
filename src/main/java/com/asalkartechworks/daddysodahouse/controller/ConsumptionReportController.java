package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.report.ConsumptionSummaryResponseDto;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.ConsumptionReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for retrieving consumption summary data and downloading consumption CSV reports.
 */
@RestController
@RequestMapping("/api/v1/reports/consumption")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Consumption Reports", description = "Endpoints for IoT telemetry and manual flavour consumption metrics and CSV exports")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
public class ConsumptionReportController {

    private final ConsumptionReportService consumptionReportService;

    @Operation(summary = "Get Consumption Summary", description = "Aggregates IoT syrup/soda usage and manual sales numbers across flavours and machines")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ConsumptionSummaryResponseDto>> getConsumptionSummary(
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "ALL") String source,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ConsumptionSummaryResponseDto summary = consumptionReportService.getConsumptionSummary(
                franchiseId, machineId, productId, fromDate, toDate, source, principal
        );
        return ResponseEntity.ok(ApiResponse.success(summary, "Consumption summary retrieved successfully"));
    }

    @Operation(summary = "Download Consumption Report (CSV)", description = "Streams downloadable CSV export of IoT dispenses and manual sales fallback entries")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadConsumptionReport(
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "ALL") String source,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        byte[] csvData = consumptionReportService.generateConsumptionReportCsv(
                franchiseId, machineId, productId, fromDate, toDate, source, principal
        );

        String filename = String.format("consumption-report-%s.csv", LocalDate.now());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvData);
    }
}
