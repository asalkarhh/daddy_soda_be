package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.inventory.StockAdjustmentRequestDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockAdjustmentType;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseCancelRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseItemRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.InventoryStock;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.Supplier;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.InventoryStockRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseEntryRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseItemRepository;
import com.asalkartechworks.daddysodahouse.repository.StockMovementRepository;
import com.asalkartechworks.daddysodahouse.repository.SupplierRepository;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import com.asalkartechworks.daddysodahouse.security.JwtService;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseInventoryIntegrationTest {

    private final MockMvc mockMvc;
    private final PurchaseEntryRepository purchaseEntryRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final FranchiseRepository franchiseRepository;
    private final OutletRepository outletRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    private User managerUser;
    private User superAdminUser;
    private User franchiseOwnerUser;
    private Supplier testSupplier;
    private Product orangeProduct;
    private Product lemonProduct;
    private String managerToken;
    private String franchiseOwnerToken;

    @Autowired
    public PurchaseInventoryIntegrationTest(
            MockMvc mockMvc,
            PurchaseEntryRepository purchaseEntryRepository,
            PurchaseItemRepository purchaseItemRepository,
            InventoryStockRepository inventoryStockRepository,
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            FranchiseRepository franchiseRepository,
            OutletRepository outletRepository,
            MachineRepository machineRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.purchaseEntryRepository = purchaseEntryRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.franchiseRepository = franchiseRepository;
        this.outletRepository = outletRepository;
        this.machineRepository = machineRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        purchaseItemRepository.deleteAll();
        purchaseEntryRepository.deleteAll();
        inventoryStockRepository.deleteAll();
        machineRepository.deleteAll();
        outletRepository.deleteAll();
        userRepository.deleteAll();
        franchiseRepository.deleteAll();
        productRepository.deleteAll();
        supplierRepository.deleteAll();

        // Seed Supplier
        testSupplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-TEST-01")
                .supplierName("Apex Flavour Ingredients")
                .mobile("9822001122")
                .isActive(true)
                .build());

        // Seed Products
        orangeProduct = productRepository.save(Product.builder()
                .itemCode("SYR-ORG-05L")
                .flavourName("Orange Syrup")
                .bottleSizeLitre(new BigDecimal("5.00"))
                .defaultSellingPrice(new BigDecimal("650.00"))
                .isActive(true)
                .build());

        lemonProduct = productRepository.save(Product.builder()
                .itemCode("SYR-LEM-05L")
                .flavourName("Lemon Syrup")
                .bottleSizeLitre(new BigDecimal("5.00"))
                .defaultSellingPrice(new BigDecimal("600.00"))
                .isActive(true)
                .build());

        // Seed Franchise & Users
        Franchise franchise = franchiseRepository.save(Franchise.builder()
                .franchiseCode("FR-TEST-01")
                .businessName("Test Franchise")
                .ownerName("Test Owner")
                .mobile("9111122222")
                .isActive(true)
                .build());

        managerUser = userRepository.save(User.builder()
                .fullName("Warehouse Manager")
                .mobile("9888888888")
                .email("manager@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Manager@12345"))
                .role(UserRole.MANAGER)
                .isActive(true)
                .build());

        superAdminUser = userRepository.save(User.builder()
                .fullName("Super Admin")
                .mobile("9999999999")
                .email("admin@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build());

        franchiseOwnerUser = userRepository.save(User.builder()
                .fullName("Franchise Owner")
                .mobile("9777777777")
                .email("owner@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Owner@12345"))
                .role(UserRole.FRANCHISE_OWNER)
                .franchise(franchise)
                .isActive(true)
                .build());

        managerToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(managerUser));
        franchiseOwnerToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(franchiseOwnerUser));
    }

    // ==========================================
    // 1. Purchase Bill Creation & Stock Inward Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/purchases successfully creates confirmed bill and increases stock")
    void createPurchase_singleItem_increasesStockAndCreatesMovement() throws Exception {
        PurchaseCreateRequestDto request = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .supplierBillNumber("BILL-1001")
                .billDate(LocalDate.now())
                .description("Initial orange syrup inward")
                .items(List.of(
                        new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("10.000"))
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.purchaseNumber").value(org.hamcrest.Matchers.startsWith("PUR-")))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(10.0))
                .andExpect(jsonPath("$.data.items[0].currentStockBalance").value(10.0))
                .andReturn();

        // Verify database state: inventory_stock
        InventoryStock stock = inventoryStockRepository.findByProductId(orangeProduct.getId()).orElseThrow();
        assertThat(stock.getAvailableQuantity()).isEqualByComparingTo(new BigDecimal("10.000"));

        // Verify database state: stock_movements
        assertThat(stockMovementRepository.searchStockMovements(orangeProduct.getId(), null, null, null, null, null).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/v1/purchases updates multiple products independently and accurately")
    void createPurchase_multiItem_increasesStockForEachProduct() throws Exception {
        PurchaseCreateRequestDto request = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .supplierBillNumber("BILL-MULTI-01")
                .billDate(LocalDate.now())
                .description("Orange and Lemon combo inward")
                .items(List.of(
                        new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("25.500")),
                        new PurchaseItemRequestDto(lemonProduct.getId(), new BigDecimal("15.250"))
                ))
                .build();

        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalQuantity").value(40.75));

        InventoryStock orangeStock = inventoryStockRepository.findByProductId(orangeProduct.getId()).orElseThrow();
        assertThat(orangeStock.getAvailableQuantity()).isEqualByComparingTo(new BigDecimal("25.500"));

        InventoryStock lemonStock = inventoryStockRepository.findByProductId(lemonProduct.getId()).orElseThrow();
        assertThat(lemonStock.getAvailableQuantity()).isEqualByComparingTo(new BigDecimal("15.250"));
    }

    @Test
    @DisplayName("POST /api/v1/purchases rejects duplicate product in the same bill")
    void createPurchase_duplicateProductInSameBill_returnsBadRequest() throws Exception {
        PurchaseCreateRequestDto request = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .billDate(LocalDate.now())
                .items(List.of(
                        new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("5.000")),
                        new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("3.000"))
                ))
                .build();

        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Duplicate product in purchase bill: " + orangeProduct.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/purchases rejects inactive product or inactive supplier")
    void createPurchase_inactiveProductOrSupplier_returnsBadRequest() throws Exception {
        // Inactive supplier test
        testSupplier.setActive(false);
        supplierRepository.save(testSupplier);

        PurchaseCreateRequestDto requestInactiveSupplier = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .billDate(LocalDate.now())
                .items(List.of(new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("5.000"))))
                .build();

        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInactiveSupplier)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot create purchase under inactive supplier: " + testSupplier.getSupplierName()));

        // Inactive product test
        testSupplier.setActive(true);
        supplierRepository.save(testSupplier);

        orangeProduct.setActive(false);
        productRepository.save(orangeProduct);

        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInactiveSupplier)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot add inactive product to purchase bill: " + orangeProduct.getFlavourName()));
    }

    @Test
    @DisplayName("POST /api/v1/purchases rejects future bill date")
    void createPurchase_futureBillDate_returnsBadRequest() throws Exception {
        PurchaseCreateRequestDto request = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .billDate(LocalDate.now().plusDays(2))
                .items(List.of(new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("5.000"))))
                .build();

        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/purchases rejects duplicate supplier bill number for same supplier")
    void createPurchase_duplicateSupplierBillNumber_returnsBadRequest() throws Exception {
        PurchaseCreateRequestDto firstBill = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .supplierBillNumber("INV-DUP-99")
                .billDate(LocalDate.now())
                .items(List.of(new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("5.000"))))
                .build();

        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBill)))
                .andExpect(status().isCreated());

        // Duplicate bill number
        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBill)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Confirmed purchase bill already exists for this supplier with bill number: INV-DUP-99"));
    }

    // ==========================================
    // 2. Purchase Cancellation & Reversal Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/purchases/{id}/cancel reverses inventory and creates reversal movement")
    void cancelPurchase_reversesStockAndLogsReversalMovement() throws Exception {
        PurchaseCreateRequestDto request = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .billDate(LocalDate.now())
                .items(List.of(new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("8.000"))))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String purchaseId = root.path("data").path("id").asText();

        PurchaseCancelRequestDto cancelDto = new PurchaseCancelRequestDto("Supplier sent wrong batch");

        mockMvc.perform(post("/api/v1/purchases/" + purchaseId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledReason").value("Supplier sent wrong batch"))
                .andExpect(jsonPath("$.data.items[0].currentStockBalance").value(0.0));

        InventoryStock stock = inventoryStockRepository.findByProductId(orangeProduct.getId()).orElseThrow();
        assertThat(stock.getAvailableQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("POST /api/v1/purchases/{id}/cancel is rejected if reversal would make stock negative")
    void cancelPurchase_failsIfReversalCausesNegativeStock() throws Exception {
        // Step 1: Create purchase for 10 units
        PurchaseCreateRequestDto request = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .billDate(LocalDate.now())
                .items(List.of(new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("10.000"))))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String purchaseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asText();

        // Step 2: Perform stock adjustment out of 5 units (remaining stock: 5 units)
        StockAdjustmentRequestDto adjOut = StockAdjustmentRequestDto.builder()
                .productId(orangeProduct.getId())
                .adjustmentType(StockAdjustmentType.ADJUSTMENT_OUT)
                .quantity(new BigDecimal("5.000"))
                .note("Sample bottles damaged")
                .build();

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjOut)))
                .andExpect(status().isCreated());

        // Step 3: Attempt to cancel original purchase of 10 units -> requires 10 units, but only 5 available
        PurchaseCancelRequestDto cancelDto = new PurchaseCancelRequestDto("Try cancelling");

        mockMvc.perform(post("/api/v1/purchases/" + purchaseId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));

        // Verify stock was not changed and remains 5
        InventoryStock stock = inventoryStockRepository.findByProductId(orangeProduct.getId()).orElseThrow();
        assertThat(stock.getAvailableQuantity()).isEqualByComparingTo(new BigDecimal("5.000"));
    }

    // ==========================================
    // 3. Stock Adjustment Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments successfully adjusts stock in and out")
    void stockAdjustment_inAndOut_updatesBalanceCorrectly() throws Exception {
        // Adjustment IN
        StockAdjustmentRequestDto adjIn = StockAdjustmentRequestDto.builder()
                .productId(lemonProduct.getId())
                .adjustmentType(StockAdjustmentType.ADJUSTMENT_IN)
                .quantity(new BigDecimal("12.000"))
                .note("Physical audit surplus found")
                .build();

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjIn)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stockBefore").value(0.0))
                .andExpect(jsonPath("$.data.stockAfter").value(12.0));

        // Adjustment OUT
        StockAdjustmentRequestDto adjOut = StockAdjustmentRequestDto.builder()
                .productId(lemonProduct.getId())
                .adjustmentType(StockAdjustmentType.ADJUSTMENT_OUT)
                .quantity(new BigDecimal("4.000"))
                .note("Spillage write-off")
                .build();

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjOut)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stockBefore").value(12.0))
                .andExpect(jsonPath("$.data.stockAfter").value(8.0));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/adjustments rejects ADJUSTMENT_OUT beyond available stock")
    void stockAdjustment_outBeyondAvailableStock_returnsBadRequest() throws Exception {
        StockAdjustmentRequestDto adjOut = StockAdjustmentRequestDto.builder()
                .productId(lemonProduct.getId())
                .adjustmentType(StockAdjustmentType.ADJUSTMENT_OUT)
                .quantity(new BigDecimal("50.000"))
                .note("Excessive reduction")
                .build();

        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjOut)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient inventory stock")));
    }

    // ==========================================
    // 4. Warehouse Inventory Query & Movement Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/inventory/warehouse returns paginated current stock")
    void getWarehouseStock_returnsStockList() throws Exception {
        // Seed some stock
        StockAdjustmentRequestDto adjIn = StockAdjustmentRequestDto.builder()
                .productId(orangeProduct.getId())
                .adjustmentType(StockAdjustmentType.ADJUSTMENT_IN)
                .quantity(new BigDecimal("10.000"))
                .note("Stock setup")
                .build();
        mockMvc.perform(post("/api/v1/inventory/adjustments")
                .header(HttpHeaders.AUTHORIZATION, managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adjIn)));

        mockMvc.perform(get("/api/v1/inventory/warehouse")
                        .param("search", "Orange")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].itemCode").value("SYR-ORG-05L"))
                .andExpect(jsonPath("$.data.content[0].availableQuantity").value(10.0));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/warehouse/{productId} returns detail and recent movements")
    void getProductInventoryDetail_returnsSummaryAndMovements() throws Exception {
        StockAdjustmentRequestDto adjIn = StockAdjustmentRequestDto.builder()
                .productId(orangeProduct.getId())
                .adjustmentType(StockAdjustmentType.ADJUSTMENT_IN)
                .quantity(new BigDecimal("5.000"))
                .note("Audit add")
                .build();
        mockMvc.perform(post("/api/v1/inventory/adjustments")
                .header(HttpHeaders.AUTHORIZATION, managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adjIn)));

        mockMvc.perform(get("/api/v1/inventory/warehouse/" + orangeProduct.getId())
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itemCode").value("SYR-ORG-05L"))
                .andExpect(jsonPath("$.data.recentMovements.length()").value(1))
                .andExpect(jsonPath("$.data.recentMovements[0].movementType").value("ADJUSTMENT_IN"));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/movements returns paginated movements")
    void getStockMovements_returnsMovementsList() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/movements")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ==========================================
    // 5. Security & Role Authorization Tests
    // ==========================================

    @Test
    @DisplayName("FRANCHISE_OWNER receives HTTP 403 Forbidden for all purchase and inventory endpoints")
    void franchiseOwner_accessingPurchaseAndInventory_returns403() throws Exception {
        // Purchases list
        mockMvc.perform(get("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken))
                .andExpect(status().isForbidden());

        // Create purchase
        PurchaseCreateRequestDto request = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .billDate(LocalDate.now())
                .items(List.of(new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("1.000"))))
                .build();
        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Inventory warehouse
        mockMvc.perform(get("/api/v1/inventory/warehouse")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken))
                .andExpect(status().isForbidden());

        // Stock adjustments
        StockAdjustmentRequestDto adj = StockAdjustmentRequestDto.builder()
                .productId(orangeProduct.getId())
                .adjustmentType(StockAdjustmentType.ADJUSTMENT_IN)
                .quantity(new BigDecimal("1.000"))
                .note("Unauthorized adjustment")
                .build();
        mockMvc.perform(post("/api/v1/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adj)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 6. Transactional Rollback Tests
    // ==========================================

    @Test
    @DisplayName("Transaction rollbacks completely if an item in multi-item purchase is invalid")
    void createPurchase_withOneInvalidItem_rollsBackEntireTransaction() throws Exception {
        UUID nonExistentProductId = UUID.randomUUID();

        PurchaseCreateRequestDto request = PurchaseCreateRequestDto.builder()
                .supplierId(testSupplier.getId())
                .billDate(LocalDate.now())
                .items(List.of(
                        new PurchaseItemRequestDto(orangeProduct.getId(), new BigDecimal("10.000")),
                        new PurchaseItemRequestDto(nonExistentProductId, new BigDecimal("5.000"))
                ))
                .build();

        mockMvc.perform(post("/api/v1/purchases")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        // Verify rollback: orangeProduct must NOT have its stock increased
        assertThat(inventoryStockRepository.findByProductId(orangeProduct.getId())).isEmpty();
        assertThat(purchaseEntryRepository.findAll()).isEmpty();
        assertThat(stockMovementRepository.findAll()).isEmpty();
    }
}
