package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.dashboard.DashboardSummaryResponseDto;
import com.asalkartechworks.daddysodahouse.dto.dashboard.DueInvoiceResponseDto;
import com.asalkartechworks.daddysodahouse.dto.dashboard.DueType;
import com.asalkartechworks.daddysodahouse.dto.dashboard.FranchiseDashboardSummaryResponseDto;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller for executive business operations dashboards and credit dues management.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Dues", description = "Operations summary, credit dues monitoring, and franchise analytics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Get overall business dashboard summary", description = "Aggregates current monthly counts, warehouse stock health, orders, revenue, and dues")
    public ResponseEntity<ApiResponse<DashboardSummaryResponseDto>> getDashboardSummary() {
        DashboardSummaryResponseDto summary = dashboardService.getOperationalSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "Dashboard summary retrieved successfully"));
    }

    @GetMapping("/dues")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(summary = "List credit due invoices with visual severity indicators", description = "Lists pending invoices classified as OVERDUE (RED), DUE_TODAY (AMBER), or UPCOMING (NORMAL). Filtered by franchise for Franchise Owners.")
    public ResponseEntity<ApiResponse<PageResponse<DueInvoiceResponseDto>>> getDues(
            @RequestParam(required = false) DueType dueType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dueDate,asc") String[] sort,
            @AuthenticationPrincipal UserPrincipal principal) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "asc"), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<DueInvoiceResponseDto> result = dashboardService.getDues(dueType, fromDate, toDate, pageable, principal);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(result), "Credit dues retrieved successfully"));
    }

    @GetMapping("/franchise-summary")
    @PreAuthorize("hasRole('FRANCHISE_OWNER')")
    @Operation(summary = "Get personalized franchise owner dashboard summary", description = "Provides active outlets, machines, orders breakdown, dues, and recent history for the authenticated franchise owner")
    public ResponseEntity<ApiResponse<FranchiseDashboardSummaryResponseDto>> getFranchiseSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        FranchiseDashboardSummaryResponseDto summary = dashboardService.getFranchiseSummary(principal);
        return ResponseEntity.ok(ApiResponse.success(summary, "Franchise summary retrieved successfully"));
    }
}
