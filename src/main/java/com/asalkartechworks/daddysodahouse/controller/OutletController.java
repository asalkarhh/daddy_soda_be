package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.common.StatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletRequestDto;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletResponseDto;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.OutletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/outlets")
@RequiredArgsConstructor
@Tag(name = "Outlet Master", description = "Outlet shop management under franchises")
@SecurityRequirement(name = "Bearer Authentication")
public class OutletController {

    private final OutletService outletService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Create outlet", description = "Create an outlet under an active franchise")
    public ResponseEntity<ApiResponse<OutletResponseDto>> createOutlet(@Valid @RequestBody OutletRequestDto dto) {
        OutletResponseDto response = outletService.createOutlet(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Outlet created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "List outlets", description = "Get paginated outlets with franchiseId, active, and search filters")
    public ResponseEntity<ApiResponse<PageResponse<OutletResponseDto>>> getAllOutlets(
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<OutletResponseDto> response = outletService.getAllOutlets(franchiseId, search, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Outlets retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(summary = "Get outlet by ID", description = "Retrieve single outlet details")
    public ResponseEntity<ApiResponse<OutletResponseDto>> getOutletById(@PathVariable UUID id) {
        OutletResponseDto response = outletService.getOutletById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Outlet retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Update outlet", description = "Update details of an existing outlet")
    public ResponseEntity<ApiResponse<OutletResponseDto>> updateOutlet(
            @PathVariable UUID id,
            @Valid @RequestBody OutletRequestDto dto) {
        OutletResponseDto response = outletService.updateOutlet(id, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Outlet updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Update outlet status", description = "Activate or deactivate an outlet (protected against deactivating outlets with active machines)")
    public ResponseEntity<ApiResponse<OutletResponseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequestDto dto) {
        OutletResponseDto response = outletService.updateStatus(id, dto.getActive());
        return ResponseEntity.ok(ApiResponse.success(response, "Outlet status updated successfully"));
    }

    @PostMapping(value = "/{id}/agreement", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Upload outlet agreement", description = "Upload or replace legal franchise agreement file for the outlet (PDF, DOC, DOCX, images, etc.)")
    public ResponseEntity<ApiResponse<OutletResponseDto>> uploadAgreement(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        OutletResponseDto response = outletService.uploadAgreement(id, file);
        return ResponseEntity.ok(ApiResponse.success(response, "Agreement file uploaded successfully"));
    }

    @GetMapping("/{id}/agreement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(summary = "Download or view outlet agreement", description = "Download or view the uploaded agreement file for this outlet")
    public ResponseEntity<byte[]> downloadAgreement(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "false") boolean view,
            @AuthenticationPrincipal UserPrincipal principal) {
        OutletService.AgreementFileDownload download = outletService.downloadAgreement(id, principal);

        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        headers.setContentType(mediaType);
        String disposition = view ? "inline" : "attachment";
        headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + download.fileName() + "\"");
        headers.setContentLength(download.content().length);

        return new ResponseEntity<>(download.content(), headers, HttpStatus.OK);
    }

    @DeleteMapping("/{id}/agreement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Remove outlet agreement", description = "Delete the uploaded agreement file for this outlet")
    public ResponseEntity<ApiResponse<OutletResponseDto>> deleteAgreement(@PathVariable UUID id) {
        OutletResponseDto response = outletService.deleteAgreement(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Agreement file removed successfully"));
    }
}
