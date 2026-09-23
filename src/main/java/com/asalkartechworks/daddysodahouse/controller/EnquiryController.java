package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.enquiry.EnquiryResponseDto;
import com.asalkartechworks.daddysodahouse.dto.enquiry.EnquiryStatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.enquiry.EnquirySummaryDto;
import com.asalkartechworks.daddysodahouse.entity.EnquiryStatus;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for managing customer franchise inquiries.
 * Restricted to SUPER_ADMIN and MANAGER.
 */
@RestController
@RequestMapping("/api/v1/enquiries")
@RequiredArgsConstructor
@Tag(name = "Enquiry Management", description = "Operations for viewing and updating website applicant inquiries")
@SecurityRequirement(name = "bearerAuth")
public class EnquiryController {

    private final EnquiryService enquiryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Search and list inquiries", description = "Filters inquiries by status, search term, and creation date range")
    public ResponseEntity<ApiResponse<PageResponse<EnquirySummaryDto>>> listEnquiries(
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc"), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<EnquirySummaryDto> result = enquiryService.searchEnquiries(status, search, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(result), "Inquiries retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Get enquiry details by ID")
    public ResponseEntity<ApiResponse<EnquiryResponseDto>> getEnquiryById(@PathVariable UUID id) {
        EnquiryResponseDto response = enquiryService.getEnquiryById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Enquiry retrieved successfully"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Update enquiry status and admin notes", description = "Transitions status from NEW to CONTACTED, or CONTACTED to CLOSED")
    public ResponseEntity<ApiResponse<EnquiryResponseDto>> updateEnquiryStatus(
            @PathVariable UUID id,
            @Valid @RequestBody EnquiryStatusUpdateRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        EnquiryResponseDto response = enquiryService.updateEnquiryStatus(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Enquiry status updated successfully"));
    }
}
