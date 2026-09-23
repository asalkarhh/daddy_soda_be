package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.inventory.InventoryProductDetailDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.InventoryStockResponseDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockAdjustmentRequestDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockAdjustmentResponseDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockMovementResponseDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockStatusFilter;
import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.InventoryService;
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
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
@Tag(name = "Warehouse Inventory", description = "Warehouse stock balance, movements, and manual adjustments (SUPER_ADMIN and MANAGER)")
@SecurityRequirement(name = "Bearer Authentication")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/warehouse")
    @Operation(summary = "List warehouse stock", description = "Get paginated warehouse stock with search, active, and IN_STOCK/OUT_OF_STOCK filters")
    public ResponseEntity<ApiResponse<PageResponse<InventoryStockResponseDto>>> getWarehouseStock(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StockStatusFilter stockStatus,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<InventoryStockResponseDto> response = inventoryService.getWarehouseStock(
                productId, search, stockStatus, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Warehouse stock retrieved successfully"));
    }

    @GetMapping("/warehouse/{productId}")
    @Operation(summary = "Get product inventory detail", description = "Retrieve stock balance and top 20 recent stock movements for a specific product")
    public ResponseEntity<ApiResponse<InventoryProductDetailDto>> getProductInventoryDetail(
            @PathVariable UUID productId) {
        InventoryProductDetailDto response = inventoryService.getProductInventoryDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(response, "Product inventory detail retrieved successfully"));
    }

    @GetMapping("/movements")
    @Operation(summary = "List stock movements", description = "Get paginated audit history of stock movements across products")
    public ResponseEntity<ApiResponse<PageResponse<StockMovementResponseDto>>> getStockMovements(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) StockMovementType movementType,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(sort = "movementAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<StockMovementResponseDto> response = inventoryService.getStockMovements(
                productId, movementType, referenceType, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock movements retrieved successfully"));
    }

    @PostMapping("/adjustments")
    @Operation(summary = "Perform stock adjustment", description = "Perform manual stock correction (ADJUSTMENT_IN or ADJUSTMENT_OUT)")
    public ResponseEntity<ApiResponse<StockAdjustmentResponseDto>> adjustStock(
            @Valid @RequestBody StockAdjustmentRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getId() : null;
        StockAdjustmentResponseDto response = inventoryService.adjustStock(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock adjustment completed successfully"));
    }
}
