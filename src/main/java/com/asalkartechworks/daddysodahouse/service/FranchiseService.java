package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.franchise.FranchiseRequestDto;
import com.asalkartechworks.daddysodahouse.dto.franchise.FranchiseResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.repository.FranchiseCreditBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FranchiseService {

    private final FranchiseRepository franchiseRepository;
    private final OutletRepository outletRepository;
    private final FranchiseCreditBalanceRepository franchiseCreditBalanceRepository;

    @Transactional
    public FranchiseResponseDto createFranchise(FranchiseRequestDto dto) {
        if (franchiseRepository.existsByFranchiseCode(dto.getFranchiseCode())) {
            throw new BadRequestException("Franchise code already exists: " + dto.getFranchiseCode());
        }

        Franchise franchise = Franchise.builder()
                .franchiseCode(dto.getFranchiseCode().trim())
                .businessName(dto.getBusinessName().trim())
                .ownerName(dto.getOwnerName().trim())
                .mobile(dto.getMobile().trim())
                .email(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null)
                .fssaiNumber(StringUtils.hasText(dto.getFssaiNumber()) ? dto.getFssaiNumber().trim() : null)
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .pincode(dto.getPincode())
                .isActive(true)
                .build();

        Franchise saved = franchiseRepository.save(franchise);
        log.info("Created franchise: {} with ID: {}", saved.getFranchiseCode(), saved.getId());
        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public FranchiseResponseDto getFranchiseById(UUID id) {
        return toResponseDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<FranchiseResponseDto> getAllFranchises(String search, Boolean active, Pageable pageable) {
        Page<Franchise> page = franchiseRepository.searchFranchises(search, active, pageable);
        return PageResponse.from(page.map(this::toResponseDto));
    }

    @Transactional
    public FranchiseResponseDto updateFranchise(UUID id, FranchiseRequestDto dto) {
        Franchise franchise = findEntityById(id);

        if (!franchise.getFranchiseCode().equalsIgnoreCase(dto.getFranchiseCode().trim())
                && franchiseRepository.existsByFranchiseCode(dto.getFranchiseCode())) {
            throw new BadRequestException("Franchise code already exists: " + dto.getFranchiseCode());
        }

        franchise.setFranchiseCode(dto.getFranchiseCode().trim());
        franchise.setBusinessName(dto.getBusinessName().trim());
        franchise.setOwnerName(dto.getOwnerName().trim());
        franchise.setMobile(dto.getMobile().trim());
        franchise.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        franchise.setFssaiNumber(StringUtils.hasText(dto.getFssaiNumber()) ? dto.getFssaiNumber().trim() : null);
        franchise.setAddress(dto.getAddress());
        franchise.setCity(dto.getCity());
        franchise.setState(dto.getState());
        franchise.setPincode(dto.getPincode());

        Franchise updated = franchiseRepository.save(franchise);
        log.info("Updated franchise ID: {}", updated.getId());
        return toResponseDto(updated);
    }

    @Transactional
    public FranchiseResponseDto updateStatus(UUID id, boolean active) {
        Franchise franchise = findEntityById(id);

        if (!active && outletRepository.existsByFranchiseIdAndIsActiveTrue(id)) {
            throw new BadRequestException("Cannot deactivate franchise with active outlets. Please deactivate or reassign its outlets first.");
        }

        franchise.setActive(active);
        Franchise updated = franchiseRepository.save(franchise);
        log.info("Franchise ID: {} status updated to active={}", updated.getId(), active);
        return toResponseDto(updated);
    }

    public Franchise findEntityById(UUID id) {
        return franchiseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", id));
    }

    public FranchiseResponseDto toResponseDto(Franchise f) {
        return FranchiseResponseDto.builder()
                .id(f.getId())
                .franchiseCode(f.getFranchiseCode())
                .businessName(f.getBusinessName())
                .ownerName(f.getOwnerName())
                .mobile(f.getMobile())
                .email(f.getEmail())
                .fssaiNumber(f.getFssaiNumber())
                .address(f.getAddress())
                .city(f.getCity())
                .state(f.getState())
                .pincode(f.getPincode())
                .active(f.isActive())
                .availableCredit(franchiseCreditBalanceRepository.findByFranchiseId(f.getId()).map(c -> c.getBalanceAmount()).orElse(BigDecimal.ZERO))
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
