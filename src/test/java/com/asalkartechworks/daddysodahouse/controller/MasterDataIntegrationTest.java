package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.common.StatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.franchise.FranchiseRequestDto;
import com.asalkartechworks.daddysodahouse.dto.machine.MachineRequestDto;
import com.asalkartechworks.daddysodahouse.dto.outlet.OutletRequestDto;
import com.asalkartechworks.daddysodahouse.dto.product.ProductRequestDto;
import com.asalkartechworks.daddysodahouse.dto.supplier.SupplierRequestDto;
import com.asalkartechworks.daddysodahouse.dto.user.UserCreateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Machine;
import com.asalkartechworks.daddysodahouse.entity.Outlet;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.Supplier;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterDataIntegrationTest {

    private final MockMvc mockMvc;
    private final FranchiseRepository franchiseRepository;
    private final OutletRepository outletRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final TestDatabaseCleaner databaseCleaner;

    private User superAdminUser;
    private User managerUser;
    private User franchiseOwnerUser;
    private Franchise testFranchise;
    private String superAdminToken;
    private String managerToken;
    private String franchiseOwnerToken;

    @Autowired
    public MasterDataIntegrationTest(
            MockMvc mockMvc,
            FranchiseRepository franchiseRepository,
            OutletRepository outletRepository,
            MachineRepository machineRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ObjectMapper objectMapper,
            TestDatabaseCleaner databaseCleaner) {
        this.mockMvc = mockMvc;
        this.franchiseRepository = franchiseRepository;
        this.outletRepository = outletRepository;
        this.machineRepository = machineRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.databaseCleaner = databaseCleaner;
    }

    @BeforeEach
    void setUp() {
        databaseCleaner.cleanAll();

        // Create base franchise for tests
        testFranchise = franchiseRepository.save(Franchise.builder()
                .franchiseCode("FR-001")
                .businessName("Daddy Soda House Pune")
                .ownerName("Rahul Patil")
                .mobile("9876543210")
                .email("rahul@daddysoda.com")
                .fssaiNumber("12345678901234")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .isActive(true)
                .build());

        // Create Super Admin
        superAdminUser = userRepository.save(User.builder()
                .fullName("System Super Admin")
                .mobile("9999999999")
                .email("superadmin@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build());

        // Create Manager
        managerUser = userRepository.save(User.builder()
                .fullName("Regional Manager")
                .mobile("8888888888")
                .email("manager@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Manager@12345"))
                .role(UserRole.MANAGER)
                .isActive(true)
                .build());

        // Create Franchise Owner
        franchiseOwnerUser = userRepository.save(User.builder()
                .fullName("Rahul Patil Owner")
                .mobile("7777777777")
                .email("owner@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Owner@12345"))
                .role(UserRole.FRANCHISE_OWNER)
                .franchise(testFranchise)
                .isActive(true)
                .build());

        superAdminToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(superAdminUser));
        managerToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(managerUser));
        franchiseOwnerToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(franchiseOwnerUser));
    }

    // ==========================================
    // 1. Franchise Master Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/franchises successfully creates franchise")
    void createFranchise_withValidData_succeeds() throws Exception {
        FranchiseRequestDto dto = FranchiseRequestDto.builder()
                .franchiseCode("FR-MUM-01")
                .businessName("Daddy Soda Mumbai Central")
                .ownerName("Suresh Desai")
                .mobile("9820098200")
                .email("suresh@desai.com")
                .fssaiNumber("11223344556677")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .build();

        mockMvc.perform(post("/api/v1/franchises")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.franchiseCode").value("FR-MUM-01"))
                .andExpect(jsonPath("$.data.businessName").value("Daddy Soda Mumbai Central"))
                .andExpect(jsonPath("$.data.active").value(true));

        assertThat(franchiseRepository.existsByFranchiseCode("FR-MUM-01")).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/franchises fails with duplicate franchise code")
    void createFranchise_withDuplicateCode_returnsBadRequest() throws Exception {
        FranchiseRequestDto dto = FranchiseRequestDto.builder()
                .franchiseCode("FR-001") // already exists in setUp
                .businessName("Another Soda House")
                .ownerName("Test Owner")
                .mobile("9876543219")
                .build();

        mockMvc.perform(post("/api/v1/franchises")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Franchise code already exists: FR-001"));
    }

    // ==========================================
    // 2. Outlet Master & Hierarchy Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/outlets creates outlet under active franchise")
    void createOutlet_underActiveFranchise_succeeds() throws Exception {
        OutletRequestDto dto = OutletRequestDto.builder()
                .franchiseId(testFranchise.getId())
                .outletCode("OUT-001")
                .outletName("FC Road Stall")
                .contactPersonName("Mahesh")
                .contactMobile("9811122233")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411004")
                .build();

        mockMvc.perform(post("/api/v1/outlets")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.outletCode").value("OUT-001"))
                .andExpect(jsonPath("$.data.franchiseId").value(testFranchise.getId().toString()))
                .andExpect(jsonPath("$.data.franchiseName").value(testFranchise.getBusinessName()));
    }

    @Test
    @DisplayName("POST /api/v1/outlets rejects creation under inactive franchise")
    void createOutlet_underInactiveFranchise_returnsBadRequest() throws Exception {
        testFranchise.setActive(false);
        franchiseRepository.save(testFranchise);

        OutletRequestDto dto = OutletRequestDto.builder()
                .franchiseId(testFranchise.getId())
                .outletCode("OUT-002")
                .outletName("JM Road Stall")
                .build();

        mockMvc.perform(post("/api/v1/outlets")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot create outlet under an inactive franchise"));
    }

    @Test
    @DisplayName("PATCH /api/v1/franchises/{id}/status rejects deactivation if active outlets exist")
    void deactivateFranchise_withActiveOutlets_failsValidation() throws Exception {
        // Create an active outlet under testFranchise
        outletRepository.save(Outlet.builder()
                .franchise(testFranchise)
                .outletCode("OUT-ACTIVE")
                .outletName("Active Outlet")
                .isActive(true)
                .build());

        StatusUpdateRequestDto statusDto = new StatusUpdateRequestDto(false);

        mockMvc.perform(patch("/api/v1/franchises/" + testFranchise.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot deactivate franchise with active outlets. Please deactivate or reassign its outlets first."));
    }

    // ==========================================
    // 3. Machine Master & Hierarchy Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/machines creates machine under active outlet")
    void createMachine_underActiveOutlet_succeeds() throws Exception {
        Outlet outlet = outletRepository.save(Outlet.builder()
                .franchise(testFranchise)
                .outletCode("OUT-M1")
                .outletName("FC Road Stall")
                .isActive(true)
                .build());

        MachineRequestDto dto = MachineRequestDto.builder()
                .outletId(outlet.getId())
                .machineCode("MAC-001")
                .machineName("FC Road 8-Valve Dispenser")
                .deviceId("IOT-DADDY-998877")
                .installationDate(LocalDate.of(2026, 3, 15))
                .build();

        mockMvc.perform(post("/api/v1/machines")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.machineCode").value("MAC-001"))
                .andExpect(jsonPath("$.data.deviceId").value("IOT-DADDY-998877"))
                .andExpect(jsonPath("$.data.outletId").value(outlet.getId().toString()));
    }

    @Test
    @DisplayName("POST /api/v1/machines rejects duplicate device ID")
    void createMachine_withDuplicateDeviceId_returnsBadRequest() throws Exception {
        Outlet outlet = outletRepository.save(Outlet.builder()
                .franchise(testFranchise)
                .outletCode("OUT-M2")
                .outletName("FC Road Stall")
                .isActive(true)
                .build());

        machineRepository.save(Machine.builder()
                .outlet(outlet)
                .machineCode("MAC-EX1")
                .deviceId("DEV-DUP-123")
                .isActive(true)
                .build());

        MachineRequestDto dto = MachineRequestDto.builder()
                .outletId(outlet.getId())
                .machineCode("MAC-EX2")
                .deviceId("DEV-DUP-123")
                .build();

        mockMvc.perform(post("/api/v1/machines")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Device ID already registered to another machine: DEV-DUP-123"));
    }

    @Test
    @DisplayName("PATCH /api/v1/outlets/{id}/status rejects deactivation if active machines exist")
    void deactivateOutlet_withActiveMachines_failsValidation() throws Exception {
        Outlet outlet = outletRepository.save(Outlet.builder()
                .franchise(testFranchise)
                .outletCode("OUT-M3")
                .outletName("FC Road Stall")
                .isActive(true)
                .build());

        machineRepository.save(Machine.builder()
                .outlet(outlet)
                .machineCode("MAC-ACTIVE")
                .isActive(true)
                .build());

        StatusUpdateRequestDto statusDto = new StatusUpdateRequestDto(false);

        mockMvc.perform(patch("/api/v1/outlets/" + outlet.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot deactivate outlet with active machines. Please deactivate or reassign its machines first."));
    }

    // ==========================================
    // 4. Product / Flavour Master Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/products creates product representing Orange Syrup 5 litre bottle")
    void createProduct_withOrangeSyrup5L_succeeds() throws Exception {
        ProductRequestDto dto = ProductRequestDto.builder()
                .itemCode("SYR-ORG-05L")
                .flavourName("Orange Syrup")
                .bottleSizeLitre(new BigDecimal("5.00"))
                .defaultSellingPrice(new BigDecimal("650.00"))
                .description("Standard 5 Litre Orange Syrup Can")
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itemCode").value("SYR-ORG-05L"))
                .andExpect(jsonPath("$.data.flavourName").value("Orange Syrup"))
                .andExpect(jsonPath("$.data.bottleSizeLitre").value(5.0))
                .andExpect(jsonPath("$.data.defaultSellingPrice").value(650.0));
    }

    @Test
    @DisplayName("POST /api/v1/products rejects invalid bottle size (<= 0) and negative selling price")
    void createProduct_withInvalidSizeAndPrice_failsValidation() throws Exception {
        ProductRequestDto invalidSizeDto = ProductRequestDto.builder()
                .itemCode("SYR-INVALID-01")
                .flavourName("Invalid Flavour")
                .bottleSizeLitre(new BigDecimal("0.00"))
                .defaultSellingPrice(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSizeDto)))
                .andExpect(status().isBadRequest());

        ProductRequestDto negativePriceDto = ProductRequestDto.builder()
                .itemCode("SYR-INVALID-02")
                .flavourName("Invalid Flavour")
                .bottleSizeLitre(new BigDecimal("5.00"))
                .defaultSellingPrice(new BigDecimal("-10.00"))
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativePriceDto)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 5. Supplier Master Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/suppliers creates supplier with valid GSTIN")
    void createSupplier_withValidData_succeeds() throws Exception {
        SupplierRequestDto dto = SupplierRequestDto.builder()
                .supplierCode("SUP-001")
                .supplierName("Beverage Flavour Tech Ltd")
                .contactPersonName("Anil K")
                .mobile("9822334455")
                .email("orders@beverageflavour.com")
                .gstin("27AAAAA0000A1Z5")
                .address("Plot 12, Bhosari MIDC, Pune")
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.supplierCode").value("SUP-001"))
                .andExpect(jsonPath("$.data.gstin").value("27AAAAA0000A1Z5"));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers rejects invalid GSTIN format")
    void createSupplier_withInvalidGstin_returnsBadRequest() throws Exception {
        SupplierRequestDto dto = SupplierRequestDto.builder()
                .supplierCode("SUP-002")
                .supplierName("Invalid Supplier")
                .gstin("INVALID-GSTIN-123")
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.gstin").value("Invalid GSTIN format (e.g. 27AAAAA0000A1Z5)"));
    }

    // ==========================================
    // 6. User Management & Franchise Assignment Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/users creates FRANCHISE_OWNER with active franchiseId")
    void createUser_franchiseOwner_withFranchiseId_succeeds() throws Exception {
        UserCreateRequestDto dto = UserCreateRequestDto.builder()
                .fullName("Sunil Gavaskar")
                .mobile("9123456789")
                .email("sunil@example.com")
                .password("Password@123")
                .role(UserRole.FRANCHISE_OWNER)
                .franchiseId(testFranchise.getId())
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Sunil Gavaskar"))
                .andExpect(jsonPath("$.data.role").value("FRANCHISE_OWNER"))
                .andExpect(jsonPath("$.data.franchiseId").value(testFranchise.getId().toString()))
                .andExpect(jsonPath("$.data.franchiseName").value(testFranchise.getBusinessName()))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/users rejects FRANCHISE_OWNER without franchiseId")
    void createUser_franchiseOwner_withoutFranchiseId_fails() throws Exception {
        UserCreateRequestDto dto = UserCreateRequestDto.builder()
                .fullName("Owner No Franchise")
                .mobile("9123456788")
                .email("nofranchise@example.com")
                .password("Password@123")
                .role(UserRole.FRANCHISE_OWNER)
                .franchiseId(null)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Franchise ID is mandatory for FRANCHISE_OWNER"));
    }

    @Test
    @DisplayName("POST /api/v1/users rejects MANAGER with franchiseId")
    void createUser_manager_withFranchiseId_fails() throws Exception {
        UserCreateRequestDto dto = UserCreateRequestDto.builder()
                .fullName("Manager With Franchise")
                .mobile("9123456787")
                .email("managerwithfr@example.com")
                .password("Password@123")
                .role(UserRole.MANAGER)
                .franchiseId(testFranchise.getId())
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Franchise ID must not be assigned to MANAGER role"));
    }

    @Test
    @DisplayName("POST /api/v1/users rejects creating SUPER_ADMIN via API")
    void createUser_superAdmin_isForbiddenByBusinessRule() throws Exception {
        UserCreateRequestDto dto = UserCreateRequestDto.builder()
                .fullName("Second Super Admin")
                .mobile("9123456786")
                .email("secondadmin@example.com")
                .password("Password@123")
                .role(UserRole.SUPER_ADMIN)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Creating SUPER_ADMIN through API is not permitted"));
    }

    // ==========================================
    // 7. Security and Role Authorization Tests
    // ==========================================

    @Test
    @DisplayName("MANAGER cannot access /api/v1/users (returns 403 Forbidden)")
    void manager_accessingUserManagement_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FRANCHISE_OWNER cannot access master franchise list (returns 403 Forbidden)")
    void franchiseOwner_accessingMasterList_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/franchises")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/outlets")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/machines")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 8. Franchise Owner Self-Service (/api/v1/my)
    // ==========================================

    @Test
    @DisplayName("FRANCHISE_OWNER accesses /api/v1/my/franchise, /outlets, and /machines successfully")
    void franchiseOwner_accessesOwnDetails_succeeds() throws Exception {
        // Create an outlet and machine under testFranchise
        Outlet outlet = outletRepository.save(Outlet.builder()
                .franchise(testFranchise)
                .outletCode("OUT-MY-1")
                .outletName("My Outlet")
                .isActive(true)
                .build());

        machineRepository.save(Machine.builder()
                .outlet(outlet)
                .machineCode("MAC-MY-1")
                .deviceId("DEV-MY-1")
                .isActive(true)
                .build());

        // Also create another franchise with an outlet and machine to verify isolation
        Franchise otherFranchise = franchiseRepository.save(Franchise.builder()
                .franchiseCode("FR-OTHER")
                .businessName("Other Franchise")
                .ownerName("Other Owner")
                .mobile("9111111111")
                .isActive(true)
                .build());

        Outlet otherOutlet = outletRepository.save(Outlet.builder()
                .franchise(otherFranchise)
                .outletCode("OUT-OTHER")
                .outletName("Other Outlet")
                .isActive(true)
                .build());

        machineRepository.save(Machine.builder()
                .outlet(otherOutlet)
                .machineCode("MAC-OTHER")
                .isActive(true)
                .build());

        // Test /api/v1/my/franchise returns only testFranchise
        mockMvc.perform(get("/api/v1/my/franchise")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.franchiseCode").value("FR-001"))
                .andExpect(jsonPath("$.data.businessName").value("Daddy Soda House Pune"));

        // Test /api/v1/my/outlets returns only OUT-MY-1
        mockMvc.perform(get("/api/v1/my/outlets")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].outletCode").value("OUT-MY-1"));

        // Test /api/v1/my/machines returns only MAC-MY-1
        mockMvc.perform(get("/api/v1/my/machines")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].machineCode").value("MAC-MY-1"));
    }

    @Test
    @DisplayName("SUPER_ADMIN and MANAGER receive HTTP 403 Forbidden on /api/v1/my/* endpoints")
    void superAdminAndManager_accessingMyEndpoints_receive403() throws Exception {
        mockMvc.perform(get("/api/v1/my/franchise")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/my/outlets")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/my/machines")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 9. Last Active Super Admin Protection & Additional Security
    // ==========================================

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status rejects deactivating the last active SUPER_ADMIN")
    void deactivateUser_lastActiveSuperAdmin_fails() throws Exception {
        StatusUpdateRequestDto dto = new StatusUpdateRequestDto(false);

        mockMvc.perform(patch("/api/v1/users/" + superAdminUser.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot deactivate the last active SUPER_ADMIN"));
    }

    @Test
    @DisplayName("FRANCHISE_OWNER cannot create franchise (returns 403 Forbidden)")
    void franchiseOwner_cannotCreateFranchise_returns403() throws Exception {
        FranchiseRequestDto dto = FranchiseRequestDto.builder()
                .franchiseCode("FR-FAIL")
                .businessName("Unauthorized")
                .ownerName("Unauthorized")
                .mobile("9123456780")
                .build();

        mockMvc.perform(post("/api/v1/franchises")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET list endpoints support search, filter, and pagination")
    void getEndpoints_supportPaginationAndSearch() throws Exception {
        mockMvc.perform(get("/api/v1/franchises")
                        .param("search", "Pune")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
