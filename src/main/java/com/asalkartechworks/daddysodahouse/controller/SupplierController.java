package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.common.StatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.supplier.SupplierRequestDto;
import com.asalkartechworks.daddysodahouse.dto.supplier.SupplierResponseDto;
import com.asalkartechworks.daddysodahouse.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
@Tag(name = "Supplier Master", description = "Raw material / syrup supplier management (SUPER_ADMIN and MANAGER)")
@SecurityRequirement(name = "Bearer Authentication")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @Operation(summary = "Create supplier", description = "Create a raw material or syrup supplier")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> createSupplier(@Valid @RequestBody SupplierRequestDto dto) {
        SupplierResponseDto response = supplierService.createSupplier(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Supplier created successfully"));
    }

    @GetMapping
    @Operation(summary = "List suppliers", description = "Get paginated suppliers with search and active status filters")
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponseDto>>> getAllSuppliers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<SupplierResponseDto> response = supplierService.getAllSuppliers(search, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Suppliers retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID", description = "Retrieve single supplier details")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> getSupplierById(@PathVariable UUID id) {
        SupplierResponseDto response = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Supplier retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update supplier", description = "Update details of an existing supplier")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierRequestDto dto) {
        SupplierResponseDto response = supplierService.updateSupplier(id, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Supplier updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update supplier status", description = "Activate or deactivate a supplier")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequestDto dto) {
        SupplierResponseDto response = supplierService.updateStatus(id, dto.getActive());
        return ResponseEntity.ok(ApiResponse.success(response, "Supplier status updated successfully"));
    }
}
