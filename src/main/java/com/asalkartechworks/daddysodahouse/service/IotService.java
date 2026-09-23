package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.config.IotProperties;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventDetailResponseDto;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventIngestRequestDto;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventIngestResponseDto;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventItemIngestRequestDto;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventItemResponseDto;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.iot.MachineConnectionStatus;
import com.asalkartechworks.daddysodahouse.dto.iot.MachineStatusResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.IotEvent;
import com.asalkartechworks.daddysodahouse.entity.IotEventItem;
import com.asalkartechworks.daddysodahouse.entity.IotEventStatus;
import com.asalkartechworks.daddysodahouse.entity.IotEventType;
import com.asalkartechworks.daddysodahouse.entity.IotItemType;
import com.asalkartechworks.daddysodahouse.entity.Machine;
import com.asalkartechworks.daddysodahouse.entity.Outlet;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.exception.AuthException;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.IotEventRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing IoT Soda Machine telemetry ingestion, validation, and administrative queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotService {

    private final IotProperties iotProperties;
    private final IotEventRepository iotEventRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    /**
     * Validates incoming machine ingestion API key in constant time.
     */
    public void validateApiKey(String providedApiKey) {
        String configuredKey = iotProperties.getApiKey();
        if (!StringUtils.hasText(providedApiKey) || !StringUtils.hasText(configuredKey)) {
            throw new AuthException("Invalid or missing API key");
        }
        boolean matches = MessageDigest.isEqual(
                providedApiKey.trim().getBytes(StandardCharsets.UTF_8),
                configuredKey.trim().getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            throw new AuthException("Invalid or missing API key");
        }
    }

    /**
     * Ingests and stores an IoT soda machine telemetry event (DRINK_DISPENSED or HEARTBEAT).
     * Enforces idempotency via eventId and updates machine last_seen_at.
     */
    @Transactional
    public IotEventIngestResponseDto ingestEvent(IotEventIngestRequestDto request, String rawJsonPayload) {
        Instant now = Instant.now();

        // 1. Idempotency Check: if eventId already processed, return duplicate response without state changes
        if (iotEventRepository.existsByExternalEventId(request.getEventId())) {
            log.info("Duplicate IoT event received and skipped: {}", request.getEventId());
            return IotEventIngestResponseDto.builder()
                    .eventId(request.getEventId())
                    .duplicate(true)
                    .machineCode(request.getMachineId())
                    .receivedAt(now)
                    .build();
        }

        // 2. Validate Machine
        Machine machine = machineRepository.findByMachineCode(request.getMachineId())
                .orElseThrow(() -> new BadRequestException("Machine not found with code: " + request.getMachineId()));

        if (!machine.isActive()) {
            throw new BadRequestException("Machine with code '" + request.getMachineId() + "' is inactive");
        }

        // 3. Validate Device ID matches machine
        if (machine.getDeviceId() == null || !machine.getDeviceId().equalsIgnoreCase(request.getDeviceId().trim())) {
            throw new BadRequestException("Device ID '" + request.getDeviceId() + "' does not match machine '" + request.getMachineId() + "'");
        }

        // 4. Validate Franchise mapping and active state
        Outlet outlet = machine.getOutlet();
        Franchise franchise = outlet != null ? outlet.getFranchise() : null;
        if (franchise == null || !franchise.getFranchiseCode().equalsIgnoreCase(request.getFranchiseId().trim())) {
            throw new BadRequestException("Machine '" + request.getMachineId() + "' does not belong to franchise code '" + request.getFranchiseId() + "'");
        }
        if (!franchise.isActive()) {
            throw new BadRequestException("Franchise with code '" + request.getFranchiseId() + "' is inactive");
        }

        // 5. Validate eventTime is not unreasonably far in future (max 10 mins ahead)
        if (request.getEventTime().isAfter(now.plus(Duration.ofMinutes(10)))) {
            throw new BadRequestException("Event time cannot be more than 10 minutes in the future");
        }

        // 6. Event Type validation & item aggregation
        BigDecimal syrupQuantityMl = BigDecimal.ZERO;
        BigDecimal rawSodaQuantityMl = BigDecimal.ZERO;
        List<IotEventItem> eventItems = new ArrayList<>();

        if (request.getEventType() == IotEventType.DRINK_DISPENSED) {
            if (!StringUtils.hasText(request.getDrinkName())) {
                throw new BadRequestException("Drink name is mandatory for DRINK_DISPENSED events");
            }
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new BadRequestException("Items must not be empty for DRINK_DISPENSED events");
            }

            boolean hasSyrup = false;
            boolean hasSoda = false;

            for (IotEventItemIngestRequestDto itemDto : request.getItems()) {
                if (itemDto.getQuantityMl() == null || itemDto.getQuantityMl().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Item quantity must be greater than zero");
                }

                Product product = null;
                if (itemDto.getItemType() == IotItemType.SYRUP) {
                    hasSyrup = true;
                    product = productRepository.findByItemCode(itemDto.getItemCode().trim())
                            .orElseThrow(() -> new BadRequestException("Syrup product not found with code: " + itemDto.getItemCode()));
                    if (!product.isActive()) {
                        throw new BadRequestException("Syrup product is inactive: " + itemDto.getItemCode());
                    }
                    syrupQuantityMl = syrupQuantityMl.add(itemDto.getQuantityMl());
                } else if (itemDto.getItemType() == IotItemType.SODA) {
                    hasSoda = true;
                    if (!"RAW_SODA".equalsIgnoreCase(itemDto.getItemCode().trim())) {
                        throw new BadRequestException("SODA item code must be RAW_SODA");
                    }
                    rawSodaQuantityMl = rawSodaQuantityMl.add(itemDto.getQuantityMl());
                }

                IotEventItem item = IotEventItem.builder()
                        .product(product)
                        .itemType(itemDto.getItemType())
                        .itemCode(itemDto.getItemCode().trim())
                        .itemName(itemDto.getItemName())
                        .lineNo(itemDto.getLineNo())
                        .quantityMl(itemDto.getQuantityMl())
                        .createdAt(now)
                        .build();
                eventItems.add(item);
            }

            if (!hasSyrup || !hasSoda) {
                throw new BadRequestException("DRINK_DISPENSED events must contain at least one SYRUP item and one SODA item");
            }
        } else if (request.getEventType() == IotEventType.HEARTBEAT) {
            // Heartbeat events can have empty items; quantities remain zero
            syrupQuantityMl = BigDecimal.ZERO;
            rawSodaQuantityMl = BigDecimal.ZERO;
        }

        // 7. Parse raw payload
        JsonNode rawPayloadNode = parseRawPayload(rawJsonPayload, request);

        // 8. Persist IotEvent and associated items
        IotEvent event = IotEvent.builder()
                .externalEventId(request.getEventId())
                .machine(machine)
                .franchise(franchise)
                .eventTime(request.getEventTime())
                .eventType(request.getEventType())
                .drinkName(request.getDrinkName())
                .eventStatus(request.getStatus())
                .syrupQuantityMl(syrupQuantityMl)
                .rawSodaQuantityMl(rawSodaQuantityMl)
                .rawPayload(rawPayloadNode)
                .receivedAt(now)
                .createdAt(now)
                .build();

        for (IotEventItem item : eventItems) {
            event.addItem(item);
        }

        iotEventRepository.save(event);

        // 9. Update machine last_seen_at
        machine.setLastSeenAt(now);
        machineRepository.save(machine);

        log.info("Saved IoT {} event {} for machine {}", request.getEventType(), request.getEventId(), machine.getMachineCode());

        return IotEventIngestResponseDto.builder()
                .eventId(request.getEventId())
                .duplicate(false)
                .machineCode(machine.getMachineCode())
                .receivedAt(now)
                .build();
    }

    /**
     * Admin/Manager API: Search paginated IoT events.
     */
    @Transactional(readOnly = true)
    public PageResponse<IotEventSummaryDto> searchEvents(
            UUID franchiseId,
            UUID machineId,
            String deviceId,
            IotEventType eventType,
            IotEventStatus eventStatus,
            UUID productId,
            Instant fromDateTime,
            Instant toDateTime,
            Pageable pageable
    ) {
        Page<IotEvent> page = iotEventRepository.searchEvents(
                franchiseId,
                machineId,
                deviceId,
                eventType,
                eventStatus,
                productId,
                fromDateTime,
                toDateTime,
                pageable
        );

        return PageResponse.from(page.map(this::mapToSummaryDto));
    }

    /**
     * Admin/Manager API: Get detailed IoT event including items and raw payload.
     */
    @Transactional(readOnly = true)
    public IotEventDetailResponseDto getEventById(UUID id) {
        IotEvent event = iotEventRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("IoT event not found with ID: " + id));

        List<IotEventItemResponseDto> items = event.getItems().stream()
                .map(item -> IotEventItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .itemType(item.getItemType())
                        .itemCode(item.getItemCode())
                        .itemName(item.getItemName())
                        .lineNo(item.getLineNo())
                        .quantityMl(item.getQuantityMl())
                        .build())
                .collect(Collectors.toList());

        return IotEventDetailResponseDto.builder()
                .id(event.getId())
                .eventId(event.getExternalEventId())
                .machineId(event.getMachine().getId())
                .machineCode(event.getMachine().getMachineCode())
                .machineName(event.getMachine().getMachineName())
                .franchiseId(event.getFranchise().getId())
                .franchiseName(event.getFranchise().getBusinessName())
                .eventTime(event.getEventTime())
                .eventType(event.getEventType())
                .drinkName(event.getDrinkName())
                .syrupQuantityMl(event.getSyrupQuantityMl())
                .rawSodaQuantityMl(event.getRawSodaQuantityMl())
                .eventStatus(event.getEventStatus())
                .receivedAt(event.getReceivedAt())
                .createdAt(event.getCreatedAt())
                .items(items)
                .rawPayload(event.getRawPayload())
                .build();
    }

    /**
     * Admin/Manager API: Return connection status for all active machines.
     */
    @Transactional(readOnly = true)
    public List<MachineStatusResponseDto> getMachineStatuses() {
        int thresholdMinutes = iotProperties.getOfflineThresholdMinutes();
        Instant now = Instant.now();
        List<Machine> machines = machineRepository.findAllByIsActiveTrueOrderByMachineCodeAsc();

        return machines.stream().map(machine -> {
            Instant lastSeen = machine.getLastSeenAt();
            MachineConnectionStatus status;
            Long minutesSince = null;

            if (lastSeen == null) {
                status = MachineConnectionStatus.NEVER_CONNECTED;
            } else {
                long diff = Duration.between(lastSeen, now).toMinutes();
                minutesSince = diff < 0 ? 0 : diff;
                status = (minutesSince <= thresholdMinutes) ? MachineConnectionStatus.ONLINE : MachineConnectionStatus.OFFLINE;
            }

            String franchiseName = (machine.getOutlet() != null && machine.getOutlet().getFranchise() != null)
                    ? machine.getOutlet().getFranchise().getBusinessName()
                    : null;

            return MachineStatusResponseDto.builder()
                    .machineId(machine.getId())
                    .machineCode(machine.getMachineCode())
                    .machineName(machine.getMachineName())
                    .franchiseName(franchiseName)
                    .deviceId(machine.getDeviceId())
                    .lastSeenAt(lastSeen)
                    .connectionStatus(status)
                    .minutesSinceLastSeen(minutesSince)
                    .build();
        }).collect(Collectors.toList());
    }

    private IotEventSummaryDto mapToSummaryDto(IotEvent event) {
        return IotEventSummaryDto.builder()
                .id(event.getId())
                .eventId(event.getExternalEventId())
                .machineCode(event.getMachine().getMachineCode())
                .machineName(event.getMachine().getMachineName())
                .franchiseName(event.getFranchise().getBusinessName())
                .eventTime(event.getEventTime())
                .eventType(event.getEventType())
                .drinkName(event.getDrinkName())
                .syrupQuantityMl(event.getSyrupQuantityMl())
                .rawSodaQuantityMl(event.getRawSodaQuantityMl())
                .eventStatus(event.getEventStatus())
                .receivedAt(event.getReceivedAt())
                .build();
    }

    private JsonNode parseRawPayload(String rawJson, Object fallbackObject) {
        if (StringUtils.hasText(rawJson)) {
            try {
                return objectMapper.readTree(rawJson);
            } catch (Exception e) {
                log.warn("Failed to parse raw JSON string, converting fallback object: {}", e.getMessage());
            }
        }
        return objectMapper.valueToTree(fallbackObject);
    }
}
