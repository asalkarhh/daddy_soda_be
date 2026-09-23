package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.franchise.FranchiseResponseDto;
import com.asalkartechworks.daddysodahouse.dto.machine.MachineResponseDto;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FranchiseOwnerSelfService {

    private final FranchiseRepository franchiseRepository;
    private final OutletRepository outletRepository;
    private final MachineRepository machineRepository;
    private final FranchiseService franchiseService;
    private final OutletService outletService;
    private final MachineService machineService;

    @Transactional(readOnly = true)
    public FranchiseResponseDto getMyFranchise(UserPrincipal principal) {
        UUID franchiseId = getVerifiedFranchiseId(principal);
        Franchise franchise = franchiseRepository.findById(franchiseId)
                .filter(Franchise::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active Franchise", "id", franchiseId));

        return franchiseService.toResponseDto(franchise);
    }

    @Transactional(readOnly = true)
    public List<OutletResponseDto> getMyOutlets(UserPrincipal principal) {
        UUID franchiseId = getVerifiedFranchiseId(principal);
        return outletRepository.findByFranchiseIdAndIsActiveTrue(franchiseId).stream()
                .map(outletService::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MachineResponseDto> getMyMachines(UserPrincipal principal) {
        UUID franchiseId = getVerifiedFranchiseId(principal);
        return machineRepository.findByOutletFranchiseIdAndIsActiveTrue(franchiseId).stream()
                .map(machineService::toResponseDto)
                .toList();
    }

    private UUID getVerifiedFranchiseId(UserPrincipal principal) {
        if (principal == null || principal.getFranchiseId() == null) {
            throw new ResourceNotFoundException("Franchise mapping", "user", principal != null ? principal.getId() : "null");
        }
        return principal.getFranchiseId();
    }
}
