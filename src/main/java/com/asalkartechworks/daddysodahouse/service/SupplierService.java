package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.supplier.SupplierRequestDto;
import com.asalkartechworks.daddysodahouse.dto.supplier.SupplierResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Supplier;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private final SupplierRepository supplierRepository;

    @Transactional
    public SupplierResponseDto createSupplier(SupplierRequestDto dto) {
        if (supplierRepository.existsBySupplierCode(dto.getSupplierCode())) {
            throw new BadRequestException("Supplier code already exists: " + dto.getSupplierCode());
        }

        validateGstinIfPresent(dto.getGstin());

        Supplier supplier = Supplier.builder()
                .supplierCode(dto.getSupplierCode().trim())
                .supplierName(dto.getSupplierName().trim())
                .contactPersonName(StringUtils.hasText(dto.getContactPersonName()) ? dto.getContactPersonName().trim() : null)
                .mobile(StringUtils.hasText(dto.getMobile()) ? dto.getMobile().trim() : null)
                .email(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null)
                .gstin(StringUtils.hasText(dto.getGstin()) ? dto.getGstin().trim() : null)
                .address(dto.getAddress())
                .isActive(true)
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Created supplier: {} with ID: {}", saved.getSupplierCode(), saved.getId());
        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public SupplierResponseDto getSupplierById(UUID id) {
        return toResponseDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponseDto> getAllSuppliers(String search, Boolean active, Pageable pageable) {
        Page<Supplier> page = supplierRepository.searchSuppliers(search, active, pageable);
        return PageResponse.from(page.map(this::toResponseDto));
    }

    @Transactional
    public SupplierResponseDto updateSupplier(UUID id, SupplierRequestDto dto) {
        Supplier supplier = findEntityById(id);

        if (!supplier.getSupplierCode().equalsIgnoreCase(dto.getSupplierCode().trim())
                && supplierRepository.existsBySupplierCode(dto.getSupplierCode())) {
            throw new BadRequestException("Supplier code already exists: " + dto.getSupplierCode());
        }

        validateGstinIfPresent(dto.getGstin());

        supplier.setSupplierCode(dto.getSupplierCode().trim());
        supplier.setSupplierName(dto.getSupplierName().trim());
        supplier.setContactPersonName(StringUtils.hasText(dto.getContactPersonName()) ? dto.getContactPersonName().trim() : null);
        supplier.setMobile(StringUtils.hasText(dto.getMobile()) ? dto.getMobile().trim() : null);
        supplier.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        supplier.setGstin(StringUtils.hasText(dto.getGstin()) ? dto.getGstin().trim() : null);
        supplier.setAddress(dto.getAddress());

        Supplier updated = supplierRepository.save(supplier);
        log.info("Updated supplier ID: {}", updated.getId());
        return toResponseDto(updated);
    }

    @Transactional
    public SupplierResponseDto updateStatus(UUID id, boolean active) {
        Supplier supplier = findEntityById(id);
        supplier.setActive(active);
        Supplier updated = supplierRepository.save(supplier);
        log.info("Supplier ID: {} status updated to active={}", updated.getId(), active);
        return toResponseDto(updated);
    }

    private void validateGstinIfPresent(String gstin) {
        if (StringUtils.hasText(gstin) && !GSTIN_PATTERN.matcher(gstin.trim()).matches()) {
            throw new BadRequestException("Invalid GSTIN format. Expected format like 27ABCDE1234F1Z5");
        }
    }

    public Supplier findEntityById(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
    }

    public SupplierResponseDto toResponseDto(Supplier s) {
        return SupplierResponseDto.builder()
                .id(s.getId())
                .supplierCode(s.getSupplierCode())
                .supplierName(s.getSupplierName())
                .contactPersonName(s.getContactPersonName())
                .mobile(s.getMobile())
                .email(s.getEmail())
                .gstin(s.getGstin())
                .address(s.getAddress())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
