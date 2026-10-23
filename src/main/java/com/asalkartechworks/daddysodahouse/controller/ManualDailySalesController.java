package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.manualsales.ManualDailySalesCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.manualsales.ManualDailySalesResponseDto;
import com.asalkartechworks.daddysodahouse.dto.manualsales.ManualDailySalesUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.ManualSalesQuantityUnit;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.ManualDailySalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for managing manual end-of-day counter entries for daily flavour sales.
 */
@RestController
@RequestMapping("/api/v1/manual-sales")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manual Daily Flavour Sales", description = "Endpoints for recording and managing end-of-day counter sales fallback")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
public class ManualDailySalesController {

    private final ManualDailySalesService manualDailySalesService;

    @Operation(summary = "Record Manual Daily Flavour Sales", description = "Records end-of-day counter reading for a machine and flavour")
    @PostMapping
    public ResponseEntity<ApiResponse<ManualDailySalesResponseDto>> createManualSales(
            @Valid @RequestBody ManualDailySalesCreateRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ManualDailySalesResponseDto result = manualDailySalesService.createManualSales(request, principal);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Manual daily sales entry recorded successfully"));
    }

    @Operation(summary = "Search Manual Daily Sales", description = "Retrieves paginated manual daily sales records with filtering")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ManualDailySalesResponseDto>>> searchManualSales(
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) ManualSalesQuantityUnit quantityUnit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "salesDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PageResponse<ManualDailySalesResponseDto> result = manualDailySalesService.searchManualSales(
                franchiseId, machineId, productId, quantityUnit, fromDate, toDate, pageable, principal
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Manual daily sales entries retrieved successfully"));
    }

    @Operation(summary = "Get Manual Daily Sales", description = "Retrieves one manual sales entry")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManualDailySalesResponseDto>> getManualSalesById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(manualDailySalesService.getManualSalesById(id, principal), "Manual daily sales entry retrieved successfully"));
    }

    @Operation(summary = "Update Manual Daily Sales", description = "Corrects quantity, unit, retail price, or notes of an existing manual sales entry")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ManualDailySalesResponseDto>> updateManualSales(
            @PathVariable UUID id,
            @Valid @RequestBody ManualDailySalesUpdateRequestDto request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ManualDailySalesResponseDto result = manualDailySalesService.updateManualSales(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(result, "Manual daily sales entry updated successfully"));
    }

    @Operation(summary = "Delete Manual Daily Sales", description = "Deletes a manual daily sales entry")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteManualSales(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        manualDailySalesService.deleteManualSales(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Manual daily sales entry deleted successfully"));
    }
}
