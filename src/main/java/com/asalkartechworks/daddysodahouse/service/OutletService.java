package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletRequestDto;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Outlet;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutletService {

    private final OutletRepository outletRepository;
    private final FranchiseRepository franchiseRepository;
    private final MachineRepository machineRepository;

    @Transactional
    public OutletResponseDto createOutlet(OutletRequestDto dto) {
        Franchise franchise = franchiseRepository.findById(dto.getFranchiseId())
                .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", dto.getFranchiseId()));

        if (!franchise.isActive()) {
            throw new BadRequestException("Cannot create outlet under an inactive franchise");
        }

        if (outletRepository.existsByOutletCode(dto.getOutletCode())) {
            throw new BadRequestException("Outlet code already exists: " + dto.getOutletCode());
        }

        Outlet outlet = Outlet.builder()
                .franchise(franchise)
                .outletCode(dto.getOutletCode().trim())
                .outletName(dto.getOutletName().trim())
                .contactPersonName(StringUtils.hasText(dto.getContactPersonName()) ? dto.getContactPersonName().trim() : null)
                .contactMobile(StringUtils.hasText(dto.getContactMobile()) ? dto.getContactMobile().trim() : null)
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .pincode(dto.getPincode())
                .isActive(true)
                .build();

        Outlet saved = outletRepository.save(outlet);
        log.info("Created outlet: {} with ID: {}", saved.getOutletCode(), saved.getId());
        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public OutletResponseDto getOutletById(UUID id) {
        return toResponseDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OutletResponseDto> getAllOutlets(UUID franchiseId, String search, Boolean active, Pageable pageable) {
        Page<Outlet> page = outletRepository.searchOutlets(franchiseId, search, active, pageable);
        return PageResponse.from(page.map(this::toResponseDto));
    }

    @Transactional
    public OutletResponseDto updateOutlet(UUID id, OutletRequestDto dto) {
        Outlet outlet = findEntityById(id);

        if (!outlet.getOutletCode().equalsIgnoreCase(dto.getOutletCode().trim())
                && outletRepository.existsByOutletCode(dto.getOutletCode())) {
            throw new BadRequestException("Outlet code already exists: " + dto.getOutletCode());
        }

        if (!outlet.getFranchise().getId().equals(dto.getFranchiseId())) {
            Franchise newFranchise = franchiseRepository.findById(dto.getFranchiseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", dto.getFranchiseId()));
            if (!newFranchise.isActive()) {
                throw new BadRequestException("Cannot assign outlet to an inactive franchise");
            }
            outlet.setFranchise(newFranchise);
        }

        outlet.setOutletCode(dto.getOutletCode().trim());
        outlet.setOutletName(dto.getOutletName().trim());
        outlet.setContactPersonName(StringUtils.hasText(dto.getContactPersonName()) ? dto.getContactPersonName().trim() : null);
        outlet.setContactMobile(StringUtils.hasText(dto.getContactMobile()) ? dto.getContactMobile().trim() : null);
        outlet.setAddress(dto.getAddress());
        outlet.setCity(dto.getCity());
        outlet.setState(dto.getState());
        outlet.setPincode(dto.getPincode());

        Outlet updated = outletRepository.save(outlet);
        log.info("Updated outlet ID: {}", updated.getId());
        return toResponseDto(updated);
    }

    @Transactional
    public OutletResponseDto updateStatus(UUID id, boolean active) {
        Outlet outlet = findEntityById(id);

        if (!active && machineRepository.existsByOutletIdAndIsActiveTrue(id)) {
            throw new BadRequestException("Cannot deactivate outlet with active machines. Please deactivate or reassign its machines first.");
        }

        outlet.setActive(active);
        Outlet updated = outletRepository.save(outlet);
        log.info("Outlet ID: {} status updated to active={}", updated.getId(), active);
        return toResponseDto(updated);
    }

    public Outlet findEntityById(UUID id) {
        return outletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outlet", "id", id));
    }

    public OutletResponseDto toResponseDto(Outlet o) {
        return OutletResponseDto.builder()
                .id(o.getId())
                .franchiseId(o.getFranchise().getId())
                .franchiseName(o.getFranchise().getBusinessName())
                .outletCode(o.getOutletCode())
                .outletName(o.getOutletName())
                .contactPersonName(o.getContactPersonName())
                .contactMobile(o.getContactMobile())
                .address(o.getAddress())
                .city(o.getCity())
                .state(o.getState())
                .pincode(o.getPincode())
                .active(o.isActive())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
