package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.iot.IotEventIngestRequestDto;
import com.asalkartechworks.daddysodahouse.dto.iot.IotEventItemIngestRequestDto;
import com.asalkartechworks.daddysodahouse.dto.manualsales.ManualDailySalesCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.manualsales.ManualDailySalesUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.InventoryStock;
import com.asalkartechworks.daddysodahouse.entity.IotEventStatus;
import com.asalkartechworks.daddysodahouse.entity.IotEventType;
import com.asalkartechworks.daddysodahouse.entity.IotItemType;
import com.asalkartechworks.daddysodahouse.entity.Machine;
import com.asalkartechworks.daddysodahouse.entity.ManualDailyFlavourSales;
import com.asalkartechworks.daddysodahouse.entity.ManualSalesQuantityUnit;
import com.asalkartechworks.daddysodahouse.entity.Outlet;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.Supplier;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.InventoryStockRepository;
import com.asalkartechworks.daddysodahouse.repository.IotEventItemRepository;
import com.asalkartechworks.daddysodahouse.repository.IotEventRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.ManualDailyFlavourSalesRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.SupplierRepository;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import com.asalkartechworks.daddysodahouse.security.JwtService;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.util.TestDatabaseCleaner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IotManualSalesIntegrationTest {

    private static final String VALID_API_KEY = "test-iot-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private FranchiseRepository franchiseRepository;

    @Autowired
    private OutletRepository outletRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private InventoryStockRepository inventoryStockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IotEventRepository iotEventRepository;

    @Autowired
    private IotEventItemRepository iotEventItemRepository;

    @Autowired
    private ManualDailyFlavourSalesRepository manualDailySalesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Franchise franchiseA;
    private Franchise franchiseB;
    private Outlet outletA;
    private Outlet outletB;
    private Machine machineA;
    private Machine machineB;
    private Product orangeSyrup;
    private Product lemonSyrup;

    private String adminToken;
    private String managerToken;
    private String franchiseOwnerTokenA;
    private String franchiseOwnerTokenB;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.cleanAll();

        // 1. Master Franchises
        franchiseA = franchiseRepository.save(Franchise.builder()
                .franchiseCode("FRAN-001")
                .businessName("Daddy Soda Pune West")
                .ownerName("Suresh Patil")
                .mobile("9822000001")
                .email("suresh@daddysoda.com")
                .isActive(true)
                .build());

        franchiseB = franchiseRepository.save(Franchise.builder()
                .franchiseCode("FRAN-002")
                .businessName("Daddy Soda Mumbai North")
                .ownerName("Ramesh Shah")
                .mobile("9822000002")
                .email("ramesh@daddysoda.com")
                .isActive(true)
                .build());

        // 2. Outlets
        outletA = outletRepository.save(Outlet.builder()
                .franchise(franchiseA)
                .outletCode("OUT-001")
                .outletName("Kothrud Depot")
                .city("Pune")
                .state("Maharashtra")
                .isActive(true)
                .build());

        outletB = outletRepository.save(Outlet.builder()
                .franchise(franchiseB)
                .outletCode("OUT-002")
                .outletName("Andheri East")
                .city("Mumbai")
                .state("Maharashtra")
                .isActive(true)
                .build());

        // 3. Machines
        machineA = machineRepository.save(Machine.builder()
                .outlet(outletA)
                .machineCode("MACH-001")
                .machineName("Dispenser 8-Line A1")
                .deviceId("DEV-001")
                .isActive(true)
                .build());

        machineB = machineRepository.save(Machine.builder()
                .outlet(outletB)
                .machineCode("MACH-002")
                .machineName("Dispenser 8-Line B1")
                .deviceId("DEV-002")
                .isActive(true)
                .build());

        // 4. Products
        orangeSyrup = productRepository.save(Product.builder()
                .itemCode("SYR-ORG")
                .flavourName("Orange Syrup")
                .bottleSizeLitre(BigDecimal.valueOf(5.0))
                .defaultSellingPrice(BigDecimal.valueOf(650.0))
                .isActive(true)
                .build());

        lemonSyrup = productRepository.save(Product.builder()
                .itemCode("SYR-LEM")
                .flavourName("Lemon Syrup")
                .bottleSizeLitre(BigDecimal.valueOf(5.0))
                .defaultSellingPrice(BigDecimal.valueOf(600.0))
                .isActive(true)
                .build());

        // 5. Initial Inventory Stock for Orange Syrup
        inventoryStockRepository.save(InventoryStock.builder()
                .product(orangeSyrup)
                .availableQuantity(BigDecimal.valueOf(100.0))
                .build());

        // 6. Users & Tokens
        User admin = userRepository.save(User.builder()
                .fullName("System Admin")
                .mobile("9999999999")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build());
        adminToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(admin));

        User manager = userRepository.save(User.builder()
                .fullName("Store Manager")
                .mobile("8888888888")
                .passwordHash(passwordEncoder.encode("Manager@123"))
                .role(UserRole.MANAGER)
                .isActive(true)
                .build());
        managerToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(manager));

        User ownerA = userRepository.save(User.builder()
                .fullName("Suresh Patil")
                .mobile("9822000001")
                .passwordHash(passwordEncoder.encode("OwnerA@123"))
                .role(UserRole.FRANCHISE_OWNER)
                .franchise(franchiseA)
                .isActive(true)
                .build());
        franchiseOwnerTokenA = "Bearer " + jwtService.generateToken(UserPrincipal.create(ownerA));

        User ownerB = userRepository.save(User.builder()
                .fullName("Ramesh Shah")
                .mobile("9822000002")
                .passwordHash(passwordEncoder.encode("OwnerB@123"))
                .role(UserRole.FRANCHISE_OWNER)
                .franchise(franchiseB)
                .isActive(true)
                .build());
        franchiseOwnerTokenB = "Bearer " + jwtService.generateToken(UserPrincipal.create(ownerB));
    }

    // =========================================================================
    // IoT Ingestion Tests
    // =========================================================================

    @Test
    @DisplayName("1. Valid DRINK_DISPENSED event is saved with item rows and raw payload")
    void ingestEvent_validDrinkDispensed_savedWithItemsAndRawPayload() throws Exception {
        IotEventIngestRequestDto request = IotEventIngestRequestDto.builder()
                .eventId("EVT-20260923-001")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now().minusSeconds(60))
                .eventType(IotEventType.DRINK_DISPENSED)
                .drinkName("Orange Soda Special")
                .status(IotEventStatus.SUCCESS)
                .items(List.of(
                        IotEventItemIngestRequestDto.builder()
                                .itemType(IotItemType.SYRUP)
                                .itemCode("SYR-ORG")
                                .itemName("Orange Syrup")
                                .lineNo(1)
                                .quantityMl(BigDecimal.valueOf(30))
                                .build(),
                        IotEventItemIngestRequestDto.builder()
                                .itemType(IotItemType.SODA)
                                .itemCode("RAW_SODA")
                                .itemName("Carbonated Water")
                                .lineNo(9)
                                .quantityMl(BigDecimal.valueOf(220))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.duplicate", is(false)))
                .andExpect(jsonPath("$.data.eventId", is("EVT-20260923-001")))
                .andExpect(jsonPath("$.data.machineCode", is("MACH-001")));

        assertThat(iotEventRepository.existsByExternalEventId("EVT-20260923-001")).isTrue();
        var rawEvent = iotEventRepository.findByExternalEventId("EVT-20260923-001").orElseThrow();
        var savedEvent = iotEventRepository.findByIdWithItems(rawEvent.getId()).orElseThrow();
        assertThat(savedEvent.getSyrupQuantityMl()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(savedEvent.getRawSodaQuantityMl()).isEqualByComparingTo(BigDecimal.valueOf(220));
        assertThat(savedEvent.getRawPayload()).isNotNull();
        assertThat(savedEvent.getItems()).hasSize(2);

        // Verify machine lastSeenAt updated
        Machine updatedMachine = machineRepository.findById(machineA.getId()).orElseThrow();
        assertThat(updatedMachine.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("2. Valid HEARTBEAT updates machine last_seen_at")
    void ingestEvent_validHeartbeat_updatesLastSeenAt() throws Exception {
        assertThat(machineA.getLastSeenAt()).isNull();

        IotEventIngestRequestDto request = IotEventIngestRequestDto.builder()
                .eventId("EVT-HB-001")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.HEARTBEAT)
                .status(IotEventStatus.SUCCESS)
                .build();

        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.duplicate", is(false)));

        Machine updatedMachine = machineRepository.findById(machineA.getId()).orElseThrow();
        assertThat(updatedMachine.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("3. Invalid API key returns 401 Unauthorized")
    void ingestEvent_invalidApiKey_returns401() throws Exception {
        IotEventIngestRequestDto request = IotEventIngestRequestDto.builder()
                .eventId("EVT-002")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.HEARTBEAT)
                .status(IotEventStatus.SUCCESS)
                .build();

        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", "wrong-secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("4. Missing API key returns 401 Unauthorized")
    void ingestEvent_missingApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/iot/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("5. Invalid machine/device/franchise mapping is rejected with 400 Bad Request")
    void ingestEvent_invalidMapping_rejectedWith400() throws Exception {
        // Machine belongs to FRAN-001, but payload specifies FRAN-002
        IotEventIngestRequestDto request = IotEventIngestRequestDto.builder()
                .eventId("EVT-MISMATCH-001")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-002") // Mismatched franchise!
                .eventTime(Instant.now())
                .eventType(IotEventType.HEARTBEAT)
                .status(IotEventStatus.SUCCESS)
                .build();

        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("does not belong to franchise code")));
    }

    @Test
    @DisplayName("6. Duplicate eventId is idempotent and does not create a second event")
    void ingestEvent_duplicateEventId_returns200WithDuplicateFlag() throws Exception {
        IotEventIngestRequestDto request = IotEventIngestRequestDto.builder()
                .eventId("EVT-IDEMPOTENT-001")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.HEARTBEAT)
                .status(IotEventStatus.SUCCESS)
                .build();

        // 1st Call
        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate", is(false)));

        long countBefore = iotEventRepository.count();

        // 2nd Call with duplicate eventId
        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate", is(true)))
                .andExpect(jsonPath("$.message", containsString("Duplicate event already processed")));

        assertThat(iotEventRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("7. Unknown/inactive syrup item code is rejected")
    void ingestEvent_unknownSyrupItemCode_rejectedWith400() throws Exception {
        IotEventIngestRequestDto request = IotEventIngestRequestDto.builder()
                .eventId("EVT-UNKNOWN-SYR")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.DRINK_DISPENSED)
                .drinkName("Unknown Soda")
                .status(IotEventStatus.SUCCESS)
                .items(List.of(
                        IotEventItemIngestRequestDto.builder()
                                .itemType(IotItemType.SYRUP)
                                .itemCode("NON_EXISTENT_FLAVOUR")
                                .quantityMl(BigDecimal.valueOf(25))
                                .build(),
                        IotEventItemIngestRequestDto.builder()
                                .itemType(IotItemType.SODA)
                                .itemCode("RAW_SODA")
                                .quantityMl(BigDecimal.valueOf(200))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Syrup product not found")));
    }

    @Test
    @DisplayName("8. Missing SYRUP or SODA item for DRINK_DISPENSED is rejected")
    void ingestEvent_missingSyrupOrSoda_rejectedWith400() throws Exception {
        // Only SYRUP item provided, no SODA item
        IotEventIngestRequestDto request = IotEventIngestRequestDto.builder()
                .eventId("EVT-NO-SODA")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.DRINK_DISPENSED)
                .drinkName("Pure Syrup Drink")
                .status(IotEventStatus.SUCCESS)
                .items(List.of(
                        IotEventItemIngestRequestDto.builder()
                                .itemType(IotItemType.SYRUP)
                                .itemCode("SYR-ORG")
                                .quantityMl(BigDecimal.valueOf(50))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must contain at least one SYRUP item and one SODA item")));
    }

    @Test
    @DisplayName("9. IoT event does not change warehouse inventory")
    void ingestEvent_doesNotModifyWarehouseInventoryStock() throws Exception {
        BigDecimal initialStock = inventoryStockRepository.findByProductId(orangeSyrup.getId()).orElseThrow().getAvailableQuantity();

        IotEventIngestRequestDto request = IotEventIngestRequestDto.builder()
                .eventId("EVT-NO-STOCK-CHANGE")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.DRINK_DISPENSED)
                .drinkName("Orange Soda")
                .status(IotEventStatus.SUCCESS)
                .items(List.of(
                        IotEventItemIngestRequestDto.builder()
                                .itemType(IotItemType.SYRUP)
                                .itemCode("SYR-ORG")
                                .quantityMl(BigDecimal.valueOf(30))
                                .build(),
                        IotEventItemIngestRequestDto.builder()
                                .itemType(IotItemType.SODA)
                                .itemCode("RAW_SODA")
                                .quantityMl(BigDecimal.valueOf(200))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/iot/events")
                        .header("X-API-KEY", VALID_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        BigDecimal stockAfter = inventoryStockRepository.findByProductId(orangeSyrup.getId()).orElseThrow().getAvailableQuantity();
        assertThat(stockAfter).isEqualByComparingTo(initialStock);
    }

    // =========================================================================
    // Connection Status Tests
    // =========================================================================

    @Test
    @DisplayName("10. Connection status returns ONLINE, OFFLINE, and NEVER_CONNECTED correctly")
    void getMachineStatuses_evaluatesConnectionStatusProperly() throws Exception {
        // machineA: just seen 5 minutes ago -> ONLINE
        machineA.setLastSeenAt(Instant.now().minus(Duration.ofMinutes(5)));
        machineRepository.save(machineA);

        // machineB: seen 45 minutes ago -> OFFLINE (threshold is 30 mins)
        machineB.setLastSeenAt(Instant.now().minus(Duration.ofMinutes(45)));
        machineRepository.save(machineB);

        // machineC: never connected
        machineRepository.save(Machine.builder()
                .outlet(outletA)
                .machineCode("MACH-003")
                .machineName("Dispenser Unconnected")
                .deviceId("DEV-003")
                .lastSeenAt(null)
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/v1/iot/machines/status")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].machineCode", is("MACH-001")))
                .andExpect(jsonPath("$.data[0].connectionStatus", is("ONLINE")))
                .andExpect(jsonPath("$.data[1].machineCode", is("MACH-002")))
                .andExpect(jsonPath("$.data[1].connectionStatus", is("OFFLINE")))
                .andExpect(jsonPath("$.data[2].machineCode", is("MACH-003")))
                .andExpect(jsonPath("$.data[2].connectionStatus", is("NEVER_CONNECTED")));
    }

    // =========================================================================
    // Manual Daily Flavour Sales Tests
    // =========================================================================

    @Test
    @DisplayName("11. Franchise Owner can enter manual sales only for own machine")
    void manualSales_franchiseOwner_canEnterOnlyForOwnMachine() throws Exception {
        // Owner A attempts to enter manual sales for machine B (which belongs to Franchise B)
        ManualDailySalesCreateRequestDto invalidRequest = ManualDailySalesCreateRequestDto.builder()
                .machineId(machineB.getId()) // Belongs to Franchise B!
                .productId(orangeSyrup.getId())
                .salesDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(45))
                .quantityUnit(ManualSalesQuantityUnit.GLASSES)
                .notes("Attempting cross-franchise entry")
                .build();

        mockMvc.perform(post("/api/v1/manual-sales")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Machine does not belong to the authorized franchise")));

        // Owner A enters for own machine A
        ManualDailySalesCreateRequestDto validRequest = ManualDailySalesCreateRequestDto.builder()
                .machineId(machineA.getId())
                .productId(orangeSyrup.getId())
                .salesDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(45))
                .quantityUnit(ManualSalesQuantityUnit.GLASSES)
                .notes("Display counter at 10 PM")
                .build();

        mockMvc.perform(post("/api/v1/manual-sales")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.franchiseId", is(franchiseA.getId().toString())))
                .andExpect(jsonPath("$.data.quantity").value(45))
                .andExpect(jsonPath("$.data.quantityUnit", is("GLASSES")));
    }

    @Test
    @DisplayName("12. Duplicate manual machine/product/date entry is rejected")
    void manualSales_duplicateEntry_rejectedWith400() throws Exception {
        ManualDailySalesCreateRequestDto request = ManualDailySalesCreateRequestDto.builder()
                .machineId(machineA.getId())
                .productId(orangeSyrup.getId())
                .salesDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(50))
                .quantityUnit(ManualSalesQuantityUnit.GLASSES)
                .build();

        // 1st entry
        mockMvc.perform(post("/api/v1/manual-sales")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2nd entry with same machine, product, and date
        mockMvc.perform(post("/api/v1/manual-sales")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists for this machine, flavour, and date")));
    }

    @Test
    @DisplayName("13. Franchise Owner cannot view raw IoT event list")
    void rawIotEvents_franchiseOwner_blockedWith403() throws Exception {
        mockMvc.perform(get("/api/v1/iot/events")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/iot/machines/status")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Consumption Report Tests
    // =========================================================================

    @Test
    @DisplayName("14. Consumption summary returns flavour-wise usage and percentages correctly")
    void consumptionSummary_returnsFlavourUsageAndPercentages() throws Exception {
        // Ingest 2 events: 75ml Orange, 25ml Lemon (Total 100ml -> Orange 75%, Lemon 25%)
        IotEventIngestRequestDto event1 = IotEventIngestRequestDto.builder()
                .eventId("EVT-REPORT-001")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.DRINK_DISPENSED)
                .drinkName("Orange Soda")
                .status(IotEventStatus.SUCCESS)
                .items(List.of(
                        IotEventItemIngestRequestDto.builder().itemType(IotItemType.SYRUP).itemCode("SYR-ORG").quantityMl(BigDecimal.valueOf(75)).build(),
                        IotEventItemIngestRequestDto.builder().itemType(IotItemType.SODA).itemCode("RAW_SODA").quantityMl(BigDecimal.valueOf(200)).build()
                ))
                .build();

        IotEventIngestRequestDto event2 = IotEventIngestRequestDto.builder()
                .eventId("EVT-REPORT-002")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.DRINK_DISPENSED)
                .drinkName("Lemon Soda")
                .status(IotEventStatus.SUCCESS)
                .items(List.of(
                        IotEventItemIngestRequestDto.builder().itemType(IotItemType.SYRUP).itemCode("SYR-LEM").quantityMl(BigDecimal.valueOf(25)).build(),
                        IotEventItemIngestRequestDto.builder().itemType(IotItemType.SODA).itemCode("RAW_SODA").quantityMl(BigDecimal.valueOf(200)).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/iot/events").header("X-API-KEY", VALID_API_KEY).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(event1))).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/iot/events").header("X-API-KEY", VALID_API_KEY).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(event2))).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/consumption/summary")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .param("source", "IOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSyrupUsageMl", is(100.0)))
                .andExpect(jsonPath("$.data.totalRawSodaUsageMl", is(400.0)))
                .andExpect(jsonPath("$.data.flavourWiseIotUsage", hasSize(2)))
                .andExpect(jsonPath("$.data.flavourWiseIotUsage[0].itemCode", is("SYR-ORG")))
                .andExpect(jsonPath("$.data.flavourWiseIotUsage[0].syrupUsageMl", is(75.0)))
                .andExpect(jsonPath("$.data.flavourWiseIotUsage[0].usagePercentage", is(75.0)))
                .andExpect(jsonPath("$.data.flavourWiseIotUsage[1].itemCode", is("SYR-LEM")))
                .andExpect(jsonPath("$.data.flavourWiseIotUsage[1].syrupUsageMl", is(25.0)))
                .andExpect(jsonPath("$.data.flavourWiseIotUsage[1].usagePercentage", is(25.0)));
    }

    @Test
    @DisplayName("15. Manual GLASSES quantity is not mixed with IoT ML usage")
    void consumptionSummary_keepsGlassesAndMlSeparate() throws Exception {
        // 1. Ingest IoT dispense (50 ml syrup)
        IotEventIngestRequestDto iotEvent = IotEventIngestRequestDto.builder()
                .eventId("EVT-SEPARATE-001")
                .machineId("MACH-001")
                .deviceId("DEV-001")
                .franchiseId("FRAN-001")
                .eventTime(Instant.now())
                .eventType(IotEventType.DRINK_DISPENSED)
                .drinkName("Orange Soda")
                .status(IotEventStatus.SUCCESS)
                .items(List.of(
                        IotEventItemIngestRequestDto.builder().itemType(IotItemType.SYRUP).itemCode("SYR-ORG").quantityMl(BigDecimal.valueOf(50)).build(),
                        IotEventItemIngestRequestDto.builder().itemType(IotItemType.SODA).itemCode("RAW_SODA").quantityMl(BigDecimal.valueOf(200)).build()
                ))
                .build();
        mockMvc.perform(post("/api/v1/iot/events").header("X-API-KEY", VALID_API_KEY).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(iotEvent))).andExpect(status().isOk());

        // 2. Record manual sale (20 GLASSES)
        ManualDailySalesCreateRequestDto manualReq = ManualDailySalesCreateRequestDto.builder()
                .machineId(machineA.getId())
                .productId(orangeSyrup.getId())
                .salesDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(20))
                .quantityUnit(ManualSalesQuantityUnit.GLASSES)
                .notes("Manual reading")
                .build();
        mockMvc.perform(post("/api/v1/manual-sales").header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(manualReq))).andExpect(status().isCreated());

        // Verify summary returns both separately
        mockMvc.perform(get("/api/v1/reports/consumption/summary")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .param("source", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSyrupUsageMl").value(50.0))
                .andExpect(jsonPath("$.data.manualQuantityTotal").value(20.0))
                .andExpect(jsonPath("$.data.manualFlavourSalesTotals[0].glassesTotal").value(20.0))
                .andExpect(jsonPath("$.data.manualFlavourSalesTotals[0].mlTotal").value(0));
    }

    @Test
    @DisplayName("16. Franchise Owner cannot download another franchise's consumption data")
    void consumptionDownload_franchiseOwner_scopedToOwnData() throws Exception {
        // Record manual sales for Franchise A and Franchise B
        manualDailySalesRepository.save(ManualDailyFlavourSales.builder()
                .franchise(franchiseA)
                .machine(machineA)
                .product(orangeSyrup)
                .salesDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(30))
                .quantityUnit(ManualSalesQuantityUnit.GLASSES)
                .notes("Franchise A sales")
                .build());

        manualDailySalesRepository.save(ManualDailyFlavourSales.builder()
                .franchise(franchiseB)
                .machine(machineB)
                .product(lemonSyrup)
                .salesDate(LocalDate.now())
                .quantity(BigDecimal.valueOf(80))
                .quantityUnit(ManualSalesQuantityUnit.GLASSES)
                .notes("Franchise B sales")
                .build());

        // Owner A requests download passing Franchise B's ID in query parameter
        MvcResult result = mockMvc.perform(get("/api/v1/reports/consumption/download")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .param("franchiseId", franchiseB.getId().toString())
                        .param("source", "MANUAL"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
                .andReturn();

        String csv = result.getResponse().getContentAsString();

        // CSV must contain Franchise A's business name and must NOT contain Franchise B's business name
        assertThat(csv).contains("Daddy Soda Pune West");
        assertThat(csv).doesNotContain("Daddy Soda Mumbai North");
    }
}
