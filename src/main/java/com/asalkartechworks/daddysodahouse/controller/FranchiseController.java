package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.common.StatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.franchise.FranchiseRequestDto;
import com.asalkartechworks.daddysodahouse.dto.franchise.FranchiseResponseDto;
import com.asalkartechworks.daddysodahouse.service.FranchiseService;
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
@RequestMapping("/api/v1/franchises")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
@Tag(name = "Franchise Master", description = "Franchise partner lifecycle management (SUPER_ADMIN and MANAGER)")
@SecurityRequirement(name = "Bearer Authentication")
public class FranchiseController {

    private final FranchiseService franchiseService;

    @PostMapping
    @Operation(summary = "Create franchise", description = "Register a new business franchise partner")
    public ResponseEntity<ApiResponse<FranchiseResponseDto>> createFranchise(@Valid @RequestBody FranchiseRequestDto dto) {
        FranchiseResponseDto response = franchiseService.createFranchise(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Franchise created successfully"));
    }

    @GetMapping
    @Operation(summary = "List franchises", description = "Get paginated list of franchises with optional search and active filters")
    public ResponseEntity<ApiResponse<PageResponse<FranchiseResponseDto>>> getAllFranchises(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<FranchiseResponseDto> response = franchiseService.getAllFranchises(search, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Franchises retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get franchise by ID", description = "Retrieve single franchise details")
    public ResponseEntity<ApiResponse<FranchiseResponseDto>> getFranchiseById(@PathVariable UUID id) {
        FranchiseResponseDto response = franchiseService.getFranchiseById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Franchise retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update franchise", description = "Update full details of an existing franchise")
    public ResponseEntity<ApiResponse<FranchiseResponseDto>> updateFranchise(
            @PathVariable UUID id,
            @Valid @RequestBody FranchiseRequestDto dto) {
        FranchiseResponseDto response = franchiseService.updateFranchise(id, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Franchise updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update franchise status", description = "Activate or deactivate a franchise (protected against deactivating franchises with active outlets)")
    public ResponseEntity<ApiResponse<FranchiseResponseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequestDto dto) {
        FranchiseResponseDto response = franchiseService.updateStatus(id, dto.getActive());
        return ResponseEntity.ok(ApiResponse.success(response, "Franchise status updated successfully"));
    }
}
