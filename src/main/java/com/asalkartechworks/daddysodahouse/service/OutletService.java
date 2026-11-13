package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.config.OutletProperties;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletRequestDto;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Outlet;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutletService {

    private final OutletRepository outletRepository;
    private final FranchiseRepository franchiseRepository;
    private final MachineRepository machineRepository;
    private final OutletProperties outletProperties;

    public record AgreementFileDownload(byte[] content, String fileName, String contentType) {}

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

    @Transactional
    public OutletResponseDto uploadAgreement(UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Agreement file cannot be empty");
        }

        Outlet outlet = findEntityById(id);

        String rawFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(rawFilename)) {
            rawFilename = "agreement-" + System.currentTimeMillis();
        }
        String cleanFileName = StringUtils.cleanPath(rawFilename);
        if (cleanFileName.contains("..")) {
            throw new BadRequestException("Filename contains invalid path sequence: " + cleanFileName);
        }

        try {
            Path baseDir = Paths.get(outletProperties.getStoragePath()).toAbsolutePath().normalize();
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }

            // Remove previous file from disk if present
            if (StringUtils.hasText(outlet.getAgreementFilePath())) {
                try {
                    Path oldPath = Paths.get(outlet.getAgreementFilePath()).toAbsolutePath().normalize();
                    Files.deleteIfExists(oldPath);
                } catch (Exception ex) {
                    log.warn("Failed to delete previous agreement file at {}: {}", outlet.getAgreementFilePath(), ex.getMessage());
                }
            }

            String diskFileName = "outlet_" + outlet.getId() + "_" + System.currentTimeMillis() + "_" + cleanFileName;
            Path targetPath = baseDir.resolve(diskFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            outlet.setAgreementFileName(cleanFileName);
            outlet.setAgreementFilePath(targetPath.toString());
            outlet.setAgreementFileType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");

            Outlet saved = outletRepository.save(outlet);
            log.info("Uploaded agreement for outlet ID: {} file: {}", saved.getId(), cleanFileName);
            return toResponseDto(saved);
        } catch (IOException e) {
            log.error("Failed to store agreement file for outlet ID: {}", id, e);
            throw new BadRequestException("Could not store agreement file: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AgreementFileDownload downloadAgreement(UUID id) {
        return downloadAgreement(id, null);
    }

    @Transactional(readOnly = true)
    public AgreementFileDownload downloadAgreement(UUID id, UserPrincipal principal) {
        Outlet outlet = findEntityById(id);

        if (principal != null && principal.getRole() == UserRole.FRANCHISE_OWNER) {
            UUID userFranchiseId = principal.getFranchiseId();
            if (userFranchiseId == null || outlet.getFranchise() == null || !outlet.getFranchise().getId().equals(userFranchiseId)) {
                log.warn("Access denied for franchise owner {} trying to access outlet {} belonging to franchise {}",
                        principal.getId(), outlet.getId(), outlet.getFranchise() != null ? outlet.getFranchise().getId() : "null");
                throw new AccessDeniedException("You do not have permission to access the agreement for this outlet.");
            }
        }

        if (!StringUtils.hasText(outlet.getAgreementFilePath())) {
            throw new ResourceNotFoundException("Agreement file not found for outlet: " + outlet.getOutletName());
        }

        try {
            Path path = Paths.get(outlet.getAgreementFilePath()).toAbsolutePath().normalize();
            if (!Files.exists(path) || !Files.isReadable(path)) {
                log.error("Agreement file exists in DB but missing on disk: {}", path);
                throw new ResourceNotFoundException("Agreement file not found on disk");
            }

            byte[] bytes = Files.readAllBytes(path);
            String fileName = StringUtils.hasText(outlet.getAgreementFileName()) ? outlet.getAgreementFileName() : "agreement";
            String contentType = StringUtils.hasText(outlet.getAgreementFileType()) ? outlet.getAgreementFileType() : "application/octet-stream";

            return new AgreementFileDownload(bytes, fileName, contentType);
        } catch (IOException e) {
            log.error("Failed to read agreement file for outlet ID: {}", id, e);
            throw new BadRequestException("Could not read agreement file: " + e.getMessage());
        }
    }

    @Transactional
    public OutletResponseDto deleteAgreement(UUID id) {
        Outlet outlet = findEntityById(id);

        if (StringUtils.hasText(outlet.getAgreementFilePath())) {
            try {
                Path path = Paths.get(outlet.getAgreementFilePath()).toAbsolutePath().normalize();
                Files.deleteIfExists(path);
            } catch (Exception ex) {
                log.warn("Failed to delete agreement file on disk for outlet {}: {}", id, ex.getMessage());
            }
        }

        outlet.setAgreementFileName(null);
        outlet.setAgreementFilePath(null);
        outlet.setAgreementFileType(null);

        Outlet saved = outletRepository.save(outlet);
        log.info("Removed agreement for outlet ID: {}", saved.getId());
        return toResponseDto(saved);
    }

    public Outlet findEntityById(UUID id) {
        return outletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outlet", "id", id));
    }

    public OutletResponseDto toResponseDto(Outlet o) {
        String contactPerson = o.getContactPersonName();
        String contactMobile = o.getContactMobile();
        String address = o.getAddress();
        String city = o.getCity();
        String state = o.getState();
        String pincode = o.getPincode();

        // Fallback to parent franchise details if omitted at the outlet level
        if (o.getFranchise() != null) {
            if (!StringUtils.hasText(contactPerson)) {
                contactPerson = o.getFranchise().getOwnerName();
            }
            if (!StringUtils.hasText(contactMobile)) {
                contactMobile = o.getFranchise().getMobile();
            }
            if (!StringUtils.hasText(address)) {
                address = o.getFranchise().getAddress();
            }
            if (!StringUtils.hasText(city)) {
                city = o.getFranchise().getCity();
            }
            if (!StringUtils.hasText(state)) {
                state = o.getFranchise().getState();
            }
            if (!StringUtils.hasText(pincode)) {
                pincode = o.getFranchise().getPincode();
            }
        }

        return OutletResponseDto.builder()
                .id(o.getId())
                .franchiseId(o.getFranchise() != null ? o.getFranchise().getId() : null)
                .franchiseName(o.getFranchise() != null ? o.getFranchise().getBusinessName() : null)
                .outletCode(o.getOutletCode())
                .outletName(o.getOutletName())
                .contactPersonName(contactPerson)
                .contactMobile(contactMobile)
                .address(address)
                .city(city)
                .state(state)
                .pincode(pincode)
                .agreementFileName(o.getAgreementFileName())
                .agreementFileType(o.getAgreementFileType())
                .hasAgreement(StringUtils.hasText(o.getAgreementFilePath()))
                .active(o.isActive())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
