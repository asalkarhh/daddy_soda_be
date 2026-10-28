package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.config.EnquiryProperties;
import com.asalkartechworks.daddysodahouse.dto.enquiry.EnquiryResponseDto;
import com.asalkartechworks.daddysodahouse.dto.enquiry.EnquiryStatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.enquiry.EnquirySummaryDto;
import com.asalkartechworks.daddysodahouse.dto.enquiry.PublicEnquiryRequestDto;
import com.asalkartechworks.daddysodahouse.dto.enquiry.PublicEnquiryResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Enquiry;
import com.asalkartechworks.daddysodahouse.entity.EnquiryStatus;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.EnquiryRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating public website inquiries, WhatsApp redirect URL generation,
 * and internal enquiry lifecycle management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final EnquiryProperties enquiryProperties;

    /**
     * Public submission of a website enquiry form. Saves record and generates WhatsApp link.
     */
    @Transactional
    public PublicEnquiryResponseDto submitPublicEnquiry(PublicEnquiryRequestDto dto) {
        String enquiryNumber = generateEnquiryNumber();

        Enquiry enquiry = Enquiry.builder()
                .enquiryNumber(enquiryNumber)
                .fullName(dto.getFullName().trim())
                .mobile(dto.getMobile().trim())
                .email(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null)
                .city(StringUtils.hasText(dto.getCity()) ? dto.getCity().trim() : null)
                .message(StringUtils.hasText(dto.getMessage()) ? dto.getMessage().trim() : null)
                .source("WEBSITE")
                .status(EnquiryStatus.NEW)
                .build();

        Enquiry saved = enquiryRepository.save(enquiry);
        log.info("Saved new website enquiry: {} for mobile: {}", saved.getEnquiryNumber(), saved.getMobile());

        String whatsAppUrl = buildWhatsAppClickToChatUrl(saved);

        return PublicEnquiryResponseDto.builder()
                .enquiryId(saved.getId())
                .enquiryNumber(saved.getEnquiryNumber())
                .whatsAppUrl(whatsAppUrl)
                .build();
    }

    /**
     * Search and list enquiries with optional filters and pagination.
     */
    @Transactional(readOnly = true)
    public Page<EnquirySummaryDto> searchEnquiries(
            EnquiryStatus status,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        Instant fromInstant = fromDate != null ? fromDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant toInstant = toDate != null ? toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        String searchTerm = StringUtils.hasText(search) ? search.trim().toLowerCase() : null;

        Specification<Enquiry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (searchTerm != null) {
                String pattern = "%" + searchTerm + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(root.get("mobile"), pattern),
                    cb.like(cb.lower(root.get("city")), pattern),
                    cb.like(cb.lower(root.get("enquiryNumber")), pattern)
                ));
            }
            if (fromInstant != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }
            if (toInstant != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toInstant));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return enquiryRepository.findAll(spec, pageable).map(this::toSummaryDto);
    }

    /**
     * Get single enquiry details by ID.
     */
    @Transactional(readOnly = true)
    public EnquiryResponseDto getEnquiryById(UUID id) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", id));
        return toResponseDto(enquiry);
    }

    /**
     * Update enquiry status and append admin notes.
     * Transitions allowed:
     * - NEW -> CONTACTED
     * - CONTACTED -> CLOSED
     * - NEW -> CLOSED allowed ONLY if SUPER_ADMIN
     */
    @Transactional
    public EnquiryResponseDto updateEnquiryStatus(UUID id, EnquiryStatusUpdateRequestDto dto, UserPrincipal principal) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", id));

        EnquiryStatus currentStatus = enquiry.getStatus();
        EnquiryStatus targetStatus = dto.getStatus();

        if (currentStatus != targetStatus) {
            if (currentStatus == EnquiryStatus.NEW && targetStatus == EnquiryStatus.CLOSED) {
                if (principal == null || principal.getRole() != UserRole.SUPER_ADMIN) {
                    throw new BadRequestException("Enquiry cannot be directly CLOSED from NEW. It must transition through CONTACTED first unless performed by Super Admin.");
                }
            } else if (currentStatus == EnquiryStatus.CLOSED && targetStatus != EnquiryStatus.CLOSED) {
                // Closing is final in V1 unless re-contacted
                log.info("Re-opening closed enquiry {} to {}", enquiry.getEnquiryNumber(), targetStatus);
            }

            if (targetStatus == EnquiryStatus.CONTACTED && enquiry.getContactedAt() == null) {
                enquiry.setContactedAt(Instant.now());
            }

            enquiry.setStatus(targetStatus);
        }

        if (StringUtils.hasText(dto.getAdminNotes())) {
            enquiry.setAdminNotes(dto.getAdminNotes().trim());
        }

        Enquiry updated = enquiryRepository.save(enquiry);
        log.info("Updated enquiry {} to status {}", updated.getEnquiryNumber(), updated.getStatus());

        return toResponseDto(updated);
    }

    private synchronized String generateEnquiryNumber() {
        LocalDate now = LocalDate.now();
        String prefix = String.format("ENQ-%d%02d-", now.getYear(), now.getMonthValue());

        return enquiryRepository.findMaxEnquiryNumberWithPrefix(prefix)
                .map(maxNumber -> {
                    try {
                        String seqStr = maxNumber.substring(prefix.length());
                        long seq = Long.parseLong(seqStr) + 1;
                        return String.format("%s%05d", prefix, seq);
                    } catch (Exception e) {
                        return String.format("%s%05d", prefix, 1);
                    }
                })
                .orElse(String.format("%s%05d", prefix, 1));
    }

    /**
     * Builds standard WhatsApp click-to-chat URL: https://wa.me/{number}?text={urlEncodedText}
     */
    public String buildWhatsAppClickToChatUrl(Enquiry enquiry) {
        String recipientNumber = enquiryProperties.getWhatsappNumber();
        if (!StringUtils.hasText(recipientNumber)) {
            recipientNumber = "919876543210";
        }
        // Clean any non-digits from configured number
        recipientNumber = recipientNumber.replaceAll("[^0-9]", "");

        StringBuilder text = new StringBuilder();
        text.append("Website Franchise Enquiry\n");
        text.append("Enquiry No: ").append(enquiry.getEnquiryNumber()).append("\n");
        text.append("Name: ").append(enquiry.getFullName()).append("\n");
        text.append("Mobile: ").append(enquiry.getMobile()).append("\n");

        if (StringUtils.hasText(enquiry.getEmail())) {
            text.append("Email: ").append(enquiry.getEmail()).append("\n");
        }
        if (StringUtils.hasText(enquiry.getCity())) {
            text.append("City: ").append(enquiry.getCity()).append("\n");
        }
        if (StringUtils.hasText(enquiry.getMessage())) {
            text.append("Message: ").append(enquiry.getMessage()).append("\n");
        }

        String encodedText = URLEncoder.encode(text.toString(), StandardCharsets.UTF_8);
        return String.format("https://wa.me/%s?text=%s", recipientNumber, encodedText);
    }

    private EnquiryResponseDto toResponseDto(Enquiry enquiry) {
        return EnquiryResponseDto.builder()
                .id(enquiry.getId())
                .enquiryNumber(enquiry.getEnquiryNumber())
                .fullName(enquiry.getFullName())
                .mobile(enquiry.getMobile())
                .email(enquiry.getEmail())
                .city(enquiry.getCity())
                .message(enquiry.getMessage())
                .source(enquiry.getSource())
                .status(enquiry.getStatus())
                .adminNotes(enquiry.getAdminNotes())
                .contactedAt(enquiry.getContactedAt())
                .createdAt(enquiry.getCreatedAt())
                .updatedAt(enquiry.getUpdatedAt())
                .createdBy(enquiry.getCreatedBy())
                .updatedBy(enquiry.getUpdatedBy())
                .whatsAppUrl(buildWhatsAppClickToChatUrl(enquiry))
                .build();
    }

    private EnquirySummaryDto toSummaryDto(Enquiry enquiry) {
        return EnquirySummaryDto.builder()
                .id(enquiry.getId())
                .enquiryNumber(enquiry.getEnquiryNumber())
                .fullName(enquiry.getFullName())
                .mobile(enquiry.getMobile())
                .email(enquiry.getEmail())
                .city(enquiry.getCity())
                .status(enquiry.getStatus())
                .contactedAt(enquiry.getContactedAt())
                .createdAt(enquiry.getCreatedAt())
                .build();
    }
}
