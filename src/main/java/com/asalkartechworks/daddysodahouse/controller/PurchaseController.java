package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseCancelRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseResponseDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseSummaryDto;
import com.asalkartechworks.daddysodahouse.entity.PurchaseStatus;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.PurchaseService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
@Tag(name = "Purchase Bills", description = "Supplier purchase bill inward and stock entries (SUPER_ADMIN and MANAGER)")
@SecurityRequirement(name = "Bearer Authentication")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    @Operation(summary = "Create purchase bill", description = "Record manual purchase bill inward and increase warehouse stock")
    public ResponseEntity<ApiResponse<PurchaseResponseDto>> createPurchase(
            @Valid @RequestBody PurchaseCreateRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getId() : null;
        PurchaseResponseDto response = purchaseService.createPurchase(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Purchase bill created successfully and stock updated"));
    }

    @GetMapping
    @Operation(summary = "List purchase bills", description = "Get paginated purchase bills with supplier, status, date, and search filters")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseSummaryDto>>> getAllPurchases(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) PurchaseStatus status,
            @RequestParam(required = false) String purchaseNumber,
            @RequestParam(required = false) String supplierBillNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PurchaseSummaryDto> response = purchaseService.getAllPurchases(
                supplierId, status, purchaseNumber, supplierBillNumber, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase bills retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase bill by ID", description = "Retrieve full purchase bill details with item stock balance")
    public ResponseEntity<ApiResponse<PurchaseResponseDto>> getPurchaseById(@PathVariable UUID id) {
        PurchaseResponseDto response = purchaseService.getPurchaseById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase bill retrieved successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel purchase bill", description = "Cancel confirmed purchase bill and reverse inventory stock")
    public ResponseEntity<ApiResponse<PurchaseResponseDto>> cancelPurchase(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseCancelRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getId() : null;
        PurchaseResponseDto response = purchaseService.cancelPurchase(id, dto, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Purchase bill cancelled successfully and stock reversed"));
    }
}
