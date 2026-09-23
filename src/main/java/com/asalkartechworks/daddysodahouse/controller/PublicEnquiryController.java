package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.enquiry.PublicEnquiryRequestDto;
import com.asalkartechworks.daddysodahouse.dto.enquiry.PublicEnquiryResponseDto;
import com.asalkartechworks.daddysodahouse.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for website visitors to submit franchise opportunities and business inquiries.
 */
@RestController
@RequestMapping("/api/v1/public/enquiries")
@RequiredArgsConstructor
@Tag(name = "Public Website Inquiries", description = "Public endpoints for franchise applicant inquiries")
public class PublicEnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping
    @Operation(
            summary = "Submit public franchise website enquiry",
            description = "Saves prospective applicant enquiry and returns prefilled WhatsApp click-to-chat URL for client redirection"
    )
    public ResponseEntity<ApiResponse<PublicEnquiryResponseDto>> submitEnquiry(
            @Valid @RequestBody PublicEnquiryRequestDto dto) {
        PublicEnquiryResponseDto response = enquiryService.submitPublicEnquiry(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Enquiry submitted successfully"));
    }
}
