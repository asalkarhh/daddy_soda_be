package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.machine.MachineRequestDto;
import com.asalkartechworks.daddysodahouse.dto.machine.MachineResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Machine;
import com.asalkartechworks.daddysodahouse.entity.Outlet;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
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
public class MachineService {

    private final MachineRepository machineRepository;
    private final OutletRepository outletRepository;

    @Transactional
    public MachineResponseDto createMachine(MachineRequestDto dto) {
        Outlet outlet = outletRepository.findById(dto.getOutletId())
                .orElseThrow(() -> new ResourceNotFoundException("Outlet", "id", dto.getOutletId()));

        if (!outlet.isActive()) {
            throw new BadRequestException("Cannot create machine under an inactive outlet");
        }

        if (machineRepository.existsByMachineCode(dto.getMachineCode())) {
            throw new BadRequestException("Machine code already exists: " + dto.getMachineCode());
        }

        if (StringUtils.hasText(dto.getDeviceId()) && machineRepository.existsByDeviceId(dto.getDeviceId().trim())) {
            throw new BadRequestException("Device ID already registered to another machine: " + dto.getDeviceId());
        }

        Machine machine = Machine.builder()
                .outlet(outlet)
                .machineCode(dto.getMachineCode().trim())
                .machineName(StringUtils.hasText(dto.getMachineName()) ? dto.getMachineName().trim() : null)
                .deviceId(StringUtils.hasText(dto.getDeviceId()) ? dto.getDeviceId().trim() : null)
                .installationDate(dto.getInstallationDate())
                .isActive(true)
                .build();

        Machine saved = machineRepository.save(machine);
        log.info("Created machine: {} with ID: {}", saved.getMachineCode(), saved.getId());
        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public MachineResponseDto getMachineById(UUID id) {
        return toResponseDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MachineResponseDto> getAllMachines(UUID outletId, UUID franchiseId, String search, Boolean active, Pageable pageable) {
        Page<Machine> page = machineRepository.searchMachines(outletId, franchiseId, search, active, pageable);
        return PageResponse.from(page.map(this::toResponseDto));
    }

    @Transactional
    public MachineResponseDto updateMachine(UUID id, MachineRequestDto dto) {
        Machine machine = findEntityById(id);

        if (!machine.getMachineCode().equalsIgnoreCase(dto.getMachineCode().trim())
                && machineRepository.existsByMachineCode(dto.getMachineCode())) {
            throw new BadRequestException("Machine code already exists: " + dto.getMachineCode());
        }

        if (StringUtils.hasText(dto.getDeviceId())) {
            String trimmedDeviceId = dto.getDeviceId().trim();
            if (machineRepository.existsByDeviceIdAndIdNot(trimmedDeviceId, id)) {
                throw new BadRequestException("Device ID already registered to another machine: " + dto.getDeviceId());
            }
            machine.setDeviceId(trimmedDeviceId);
        } else {
            machine.setDeviceId(null);
        }

        if (!machine.getOutlet().getId().equals(dto.getOutletId())) {
            Outlet newOutlet = outletRepository.findById(dto.getOutletId())
                    .orElseThrow(() -> new ResourceNotFoundException("Outlet", "id", dto.getOutletId()));
            if (!newOutlet.isActive()) {
                throw new BadRequestException("Cannot assign machine to an inactive outlet");
            }
            machine.setOutlet(newOutlet);
        }

        machine.setMachineCode(dto.getMachineCode().trim());
        machine.setMachineName(StringUtils.hasText(dto.getMachineName()) ? dto.getMachineName().trim() : null);
        machine.setInstallationDate(dto.getInstallationDate());

        Machine updated = machineRepository.save(machine);
        log.info("Updated machine ID: {}", updated.getId());
        return toResponseDto(updated);
    }

    @Transactional
    public MachineResponseDto updateStatus(UUID id, boolean active) {
        Machine machine = findEntityById(id);
        machine.setActive(active);
        Machine updated = machineRepository.save(machine);
        log.info("Machine ID: {} status updated to active={}", updated.getId(), active);
        return toResponseDto(updated);
    }

    public Machine findEntityById(UUID id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine", "id", id));
    }

    public MachineResponseDto toResponseDto(Machine m) {
        return MachineResponseDto.builder()
                .id(m.getId())
                .outletId(m.getOutlet().getId())
                .outletName(m.getOutlet().getOutletName())
                .franchiseId(m.getOutlet().getFranchise().getId())
                .franchiseName(m.getOutlet().getFranchise().getBusinessName())
                .machineCode(m.getMachineCode())
                .machineName(m.getMachineName())
                .deviceId(m.getDeviceId())
                .installationDate(m.getInstallationDate())
                .lastSeenAt(m.getLastSeenAt())
                .active(m.isActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
