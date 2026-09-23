package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.manualsales.ManualDailySalesCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.manualsales.ManualDailySalesResponseDto;
import com.asalkartechworks.daddysodahouse.dto.manualsales.ManualDailySalesUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Machine;
import com.asalkartechworks.daddysodahouse.entity.ManualDailyFlavourSales;
import com.asalkartechworks.daddysodahouse.entity.ManualSalesQuantityUnit;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.AuthException;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.ManualDailyFlavourSalesRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service managing manual end-of-day counter entries for daily flavour sales.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManualDailySalesService {

    private final ManualDailyFlavourSalesRepository manualDailySalesRepository;
    private final FranchiseRepository franchiseRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;

    /**
     * Records a new manual daily sales entry.
     */
    @Transactional
    public ManualDailySalesResponseDto createManualSales(ManualDailySalesCreateRequestDto request, UserPrincipal principal) {
        UUID effectiveFranchiseId = resolveEffectiveFranchiseId(request.getFranchiseId(), principal);

        Franchise franchise = franchiseRepository.findById(effectiveFranchiseId)
                .orElseThrow(() -> new ResourceNotFoundException("Franchise not found with ID: " + effectiveFranchiseId));

        if (!franchise.isActive()) {
            throw new BadRequestException("Franchise is inactive");
        }

        // Validate Machine
        Machine machine = machineRepository.findById(request.getMachineId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine not found with ID: " + request.getMachineId()));

        if (!machine.isActive()) {
            throw new BadRequestException("Machine is inactive");
        }

        // Verify machine belongs to the effective franchise
        if (machine.getOutlet() == null || machine.getOutlet().getFranchise() == null ||
                !machine.getOutlet().getFranchise().getId().equals(effectiveFranchiseId)) {
            throw new BadRequestException("Machine does not belong to the authorized franchise");
        }

        // Validate Product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Product is inactive: " + product.getFlavourName());
        }

        // Validate Date (cannot be future)
        if (request.getSalesDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Sales date cannot be in the future");
        }

        // Validate Quantity
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        // Prevent Duplicate Entry
        if (manualDailySalesRepository.existsByMachineIdAndProductIdAndSalesDate(
                machine.getId(), product.getId(), request.getSalesDate())) {
            throw new BadRequestException("A manual sales entry already exists for this machine, flavour, and date");
        }

        ManualSalesQuantityUnit unit = request.getQuantityUnit() != null
                ? request.getQuantityUnit()
                : ManualSalesQuantityUnit.GLASSES;

        ManualDailyFlavourSales entry = ManualDailyFlavourSales.builder()
                .franchise(franchise)
                .machine(machine)
                .product(product)
                .salesDate(request.getSalesDate())
                .quantity(request.getQuantity())
                .quantityUnit(unit)
                .notes(request.getNotes())
                .build();

        ManualDailyFlavourSales saved = manualDailySalesRepository.save(entry);
        log.info("Saved manual daily sales entry {} for machine {} on {}", saved.getId(), machine.getMachineCode(), request.getSalesDate());

        return mapToDto(saved);
    }

    /**
     * Updates an existing manual sales entry.
     */
    @Transactional
    public ManualDailySalesResponseDto updateManualSales(UUID id, ManualDailySalesUpdateRequestDto request, UserPrincipal principal) {
        ManualDailyFlavourSales entry = manualDailySalesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manual sales entry not found with ID: " + id));

        // Enforce Franchise Owner ownership
        verifyRecordOwnership(entry, principal);

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        entry.setQuantity(request.getQuantity());
        entry.setQuantityUnit(request.getQuantityUnit());
        entry.setNotes(request.getNotes());

        ManualDailyFlavourSales updated = manualDailySalesRepository.save(entry);
        return mapToDto(updated);
    }

    /**
     * Searches manual daily sales entries with filtering and pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<ManualDailySalesResponseDto> searchManualSales(
            UUID requestedFranchiseId,
            UUID machineId,
            UUID productId,
            ManualSalesQuantityUnit quantityUnit,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable,
            UserPrincipal principal
    ) {
        UUID effectiveFranchiseId;
        if (principal.getRole() == UserRole.FRANCHISE_OWNER) {
            effectiveFranchiseId = principal.getFranchiseId();
        } else {
            effectiveFranchiseId = requestedFranchiseId;
        }

        Page<ManualDailyFlavourSales> page = manualDailySalesRepository.searchManualSales(
                effectiveFranchiseId,
                machineId,
                productId,
                quantityUnit,
                fromDate,
                toDate,
                pageable
        );

        return PageResponse.from(page.map(this::mapToDto));
    }

    /**
     * Deletes a manual daily sales entry.
     */
    @Transactional
    public void deleteManualSales(UUID id, UserPrincipal principal) {
        ManualDailyFlavourSales entry = manualDailySalesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manual sales entry not found with ID: " + id));

        verifyRecordOwnership(entry, principal);

        manualDailySalesRepository.delete(entry);
        log.info("Deleted manual daily sales entry {} by user {}", id, principal.getUsername());
    }

    private UUID resolveEffectiveFranchiseId(UUID requestedFranchiseId, UserPrincipal principal) {
        if (principal.getRole() == UserRole.FRANCHISE_OWNER) {
            if (principal.getFranchiseId() == null) {
                throw new AuthException("Franchise Owner is not assigned to a franchise");
            }
            return principal.getFranchiseId();
        }

        if (requestedFranchiseId == null) {
            throw new BadRequestException("Franchise ID is mandatory for Administrator and Manager entries");
        }
        return requestedFranchiseId;
    }

    private void verifyRecordOwnership(ManualDailyFlavourSales entry, UserPrincipal principal) {
        if (principal.getRole() == UserRole.FRANCHISE_OWNER) {
            if (!entry.getFranchise().getId().equals(principal.getFranchiseId())) {
                throw new BadRequestException("You are not authorized to modify records of another franchise");
            }
        }
    }

    private ManualDailySalesResponseDto mapToDto(ManualDailyFlavourSales entry) {
        return ManualDailySalesResponseDto.builder()
                .id(entry.getId())
                .franchiseId(entry.getFranchise().getId())
                .franchiseName(entry.getFranchise().getBusinessName())
                .machineId(entry.getMachine().getId())
                .machineCode(entry.getMachine().getMachineCode())
                .machineName(entry.getMachine().getMachineName())
                .productId(entry.getProduct().getId())
                .productCode(entry.getProduct().getItemCode())
                .flavourName(entry.getProduct().getFlavourName())
                .salesDate(entry.getSalesDate())
                .quantity(entry.getQuantity())
                .quantityUnit(entry.getQuantityUnit())
                .notes(entry.getNotes())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .createdBy(entry.getCreatedBy())
                .build();
    }
}
