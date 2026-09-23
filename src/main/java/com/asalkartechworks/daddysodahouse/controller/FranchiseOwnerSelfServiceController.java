package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.franchise.FranchiseResponseDto;
import com.asalkartechworks.daddysodahouse.dto.machine.MachineResponseDto;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletResponseDto;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.FranchiseOwnerSelfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/my")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FRANCHISE_OWNER')")
@Tag(name = "Franchise Owner Self-Service", description = "Dedicated self-service read endpoints for logged-in FRANCHISE_OWNER only")
@SecurityRequirement(name = "Bearer Authentication")
public class FranchiseOwnerSelfServiceController {

    private final FranchiseOwnerSelfService selfService;

    @GetMapping("/franchise")
    @Operation(summary = "Get my franchise", description = "Retrieve active franchise details of the logged-in franchise owner")
    public ResponseEntity<ApiResponse<FranchiseResponseDto>> getMyFranchise(@AuthenticationPrincipal UserPrincipal principal) {
        FranchiseResponseDto response = selfService.getMyFranchise(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Franchise details retrieved successfully"));
    }

    @GetMapping("/outlets")
    @Operation(summary = "Get my outlets", description = "Retrieve list of active outlets belonging to the logged-in franchise owner's franchise")
    public ResponseEntity<ApiResponse<List<OutletResponseDto>>> getMyOutlets(@AuthenticationPrincipal UserPrincipal principal) {
        List<OutletResponseDto> response = selfService.getMyOutlets(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Outlets retrieved successfully"));
    }

    @GetMapping("/machines")
    @Operation(summary = "Get my machines", description = "Retrieve list of active machines belonging to the logged-in franchise owner's outlets")
    public ResponseEntity<ApiResponse<List<MachineResponseDto>>> getMyMachines(@AuthenticationPrincipal UserPrincipal principal) {
        List<MachineResponseDto> response = selfService.getMyMachines(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Machines retrieved successfully"));
    }
}
