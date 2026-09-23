package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.common.StatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.machine.MachineRequestDto;
import com.asalkartechworks.daddysodahouse.dto.machine.MachineResponseDto;
import com.asalkartechworks.daddysodahouse.service.MachineService;
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
@RequestMapping("/api/v1/machines")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
@Tag(name = "Machine Master", description = "Soda dispensing machine management mapped to outlets (SUPER_ADMIN and MANAGER)")
@SecurityRequirement(name = "Bearer Authentication")
public class MachineController {

    private final MachineService machineService;

    @PostMapping
    @Operation(summary = "Create machine", description = "Create a soda machine under an active outlet")
    public ResponseEntity<ApiResponse<MachineResponseDto>> createMachine(@Valid @RequestBody MachineRequestDto dto) {
        MachineResponseDto response = machineService.createMachine(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Machine created successfully"));
    }

    @GetMapping
    @Operation(summary = "List machines", description = "Get paginated machines with outletId, franchiseId, active, and search filters")
    public ResponseEntity<ApiResponse<PageResponse<MachineResponseDto>>> getAllMachines(
            @RequestParam(required = false) UUID outletId,
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MachineResponseDto> response = machineService.getAllMachines(outletId, franchiseId, search, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Machines retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get machine by ID", description = "Retrieve single machine details")
    public ResponseEntity<ApiResponse<MachineResponseDto>> getMachineById(@PathVariable UUID id) {
        MachineResponseDto response = machineService.getMachineById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Machine retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update machine", description = "Update details of an existing machine")
    public ResponseEntity<ApiResponse<MachineResponseDto>> updateMachine(
            @PathVariable UUID id,
            @Valid @RequestBody MachineRequestDto dto) {
        MachineResponseDto response = machineService.updateMachine(id, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Machine updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update machine status", description = "Activate or deactivate a machine")
    public ResponseEntity<ApiResponse<MachineResponseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequestDto dto) {
        MachineResponseDto response = machineService.updateStatus(id, dto.getActive());
        return ResponseEntity.ok(ApiResponse.success(response, "Machine status updated successfully"));
    }
}
