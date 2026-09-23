package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.order.OrderCancelRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderItemRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.InventoryStock;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.SalesOrder;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.entity.StockMovement;
import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.InventoryStockRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseEntryRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseItemRepository;
import com.asalkartechworks.daddysodahouse.repository.SalesOrderItemRepository;
import com.asalkartechworks.daddysodahouse.repository.SalesOrderRepository;
import com.asalkartechworks.daddysodahouse.repository.StockMovementRepository;
import com.asalkartechworks.daddysodahouse.repository.SupplierRepository;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import com.asalkartechworks.daddysodahouse.security.JwtService;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.InventoryService;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesOrderIntegrationTest {

    private final MockMvc mockMvc;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final PurchaseEntryRepository purchaseEntryRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final MachineRepository machineRepository;
    private final OutletRepository outletRepository;
    private final UserRepository userRepository;
    private final FranchiseRepository franchiseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;

    private Franchise franchiseA;
    private Franchise franchiseB;
    private User superAdminUser;
    private User managerUser;
    private User franchiseOwnerUserA;
    private User franchiseOwnerUserB;
    private Product orangeProduct;
    private Product lemonProduct;
    private Product mangoProduct;

    private String superAdminToken;
    private String managerToken;
    private String franchiseOwnerTokenA;
    private String franchiseOwnerTokenB;

    @Autowired
    public SalesOrderIntegrationTest(
            MockMvc mockMvc,
            SalesOrderRepository salesOrderRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            InventoryStockRepository inventoryStockRepository,
            StockMovementRepository stockMovementRepository,
            PurchaseItemRepository purchaseItemRepository,
            PurchaseEntryRepository purchaseEntryRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            MachineRepository machineRepository,
            OutletRepository outletRepository,
            UserRepository userRepository,
            FranchiseRepository franchiseRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ObjectMapper objectMapper,
            InventoryService inventoryService) {
        this.mockMvc = mockMvc;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.purchaseEntryRepository = purchaseEntryRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.machineRepository = machineRepository;
        this.outletRepository = outletRepository;
        this.userRepository = userRepository;
        this.franchiseRepository = franchiseRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
    }

    @BeforeEach
    void setUp() {
        // Clean up in reverse foreign key order
        stockMovementRepository.deleteAll();
        salesOrderItemRepository.deleteAll();
        salesOrderRepository.deleteAll();
        purchaseItemRepository.deleteAll();
        purchaseEntryRepository.deleteAll();
        inventoryStockRepository.deleteAll();
        machineRepository.deleteAll();
        outletRepository.deleteAll();
        userRepository.deleteAll();
        franchiseRepository.deleteAll();
        productRepository.deleteAll();
        supplierRepository.deleteAll();

        // Seed Franchises
        franchiseA = franchiseRepository.save(Franchise.builder()
                .franchiseCode("FR-TEST-A")
                .businessName("Franchise Pune Station")
                .ownerName("Rohan Patil")
                .mobile("9111100001")
                .email("rohan@franchisea.com")
                .isActive(true)
                .build());

        franchiseB = franchiseRepository.save(Franchise.builder()
                .franchiseCode("FR-TEST-B")
                .businessName("Franchise Kothrud")
                .ownerName("Suresh Deshmukh")
                .mobile("9111100002")
                .email("suresh@franchiseb.com")
                .isActive(true)
                .build());

        // Seed Products
        orangeProduct = productRepository.save(Product.builder()
                .itemCode("SYR-ORG-05L")
                .flavourName("Orange Syrup")
                .bottleSizeLitre(new BigDecimal("5.00"))
                .defaultSellingPrice(new BigDecimal("500.00"))
                .isActive(true)
                .build());

        lemonProduct = productRepository.save(Product.builder()
                .itemCode("SYR-LEM-05L")
                .flavourName("Lemon Syrup")
                .bottleSizeLitre(new BigDecimal("5.00"))
                .defaultSellingPrice(new BigDecimal("450.00"))
                .isActive(true)
                .build());

        mangoProduct = productRepository.save(Product.builder()
                .itemCode("SYR-MNG-05L")
                .flavourName("Mango Syrup")
                .bottleSizeLitre(new BigDecimal("5.00"))
                .defaultSellingPrice(new BigDecimal("550.00"))
                .isActive(true)
                .build());

        // Seed Users
        superAdminUser = userRepository.save(User.builder()
                .fullName("Super Admin")
                .mobile("9000000001")
                .email("superadmin@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Admin@12345"))
                .role(UserRole.SUPER_ADMIN)
                .isActive(true)
                .build());

        managerUser = userRepository.save(User.builder()
                .fullName("Operations Manager")
                .mobile("9000000002")
                .email("manager@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Manager@12345"))
                .role(UserRole.MANAGER)
                .isActive(true)
                .build());

        franchiseOwnerUserA = userRepository.save(User.builder()
                .fullName("Rohan Patil")
                .mobile("9111100001")
                .email("owner.a@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Owner@12345"))
                .role(UserRole.FRANCHISE_OWNER)
                .franchise(franchiseA)
                .isActive(true)
                .build());

        franchiseOwnerUserB = userRepository.save(User.builder()
                .fullName("Suresh Deshmukh")
                .mobile("9111100002")
                .email("owner.b@daddysoda.com")
                .passwordHash(passwordEncoder.encode("Owner@12345"))
                .role(UserRole.FRANCHISE_OWNER)
                .franchise(franchiseB)
                .isActive(true)
                .build());

        // Generate JWT Tokens
        superAdminToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(superAdminUser));
        managerToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(managerUser));
        franchiseOwnerTokenA = "Bearer " + jwtService.generateToken(UserPrincipal.create(franchiseOwnerUserA));
        franchiseOwnerTokenB = "Bearer " + jwtService.generateToken(UserPrincipal.create(franchiseOwnerUserB));
    }

    @Test
    @DisplayName("Manager creates order with manual unit price override")
    void managerCreatesOrderWithPriceOverride() throws Exception {
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .notes("Manager special order with negotiated discount")
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(orangeProduct.getId())
                                .quantity(new BigDecimal("10.000"))
                                .unitPrice(new BigDecimal("420.00")) // Product default is 500.00
                                .build(),
                        OrderItemRequestDto.builder()
                                .productId(lemonProduct.getId())
                                .quantity(new BigDecimal("5.000"))
                                .unitPrice(new BigDecimal("400.00")) // Product default is 450.00
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.franchiseId").value(franchiseA.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("PLACED"))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(420.0))
                .andExpect(jsonPath("$.data.items[0].lineTotal").value(4200.0))
                .andExpect(jsonPath("$.data.items[1].unitPrice").value(400.0))
                .andExpect(jsonPath("$.data.items[1].lineTotal").value(2000.0))
                .andExpect(jsonPath("$.data.totalQuantity").value(15.0))
                .andExpect(jsonPath("$.data.orderTotal").value(6200.0));
    }

    @Test
    @DisplayName("Franchise owner creates order using default product price")
    void franchiseOwnerCreatesOrderUsingDefaultPrice() throws Exception {
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .notes("Weekly refill order")
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(orangeProduct.getId())
                                .quantity(new BigDecimal("4.000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.franchiseId").value(franchiseA.getId().toString()))
                .andExpect(jsonPath("$.data.franchiseCode").value("FR-TEST-A"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(500.0))
                .andExpect(jsonPath("$.data.items[0].lineTotal").value(2000.0))
                .andExpect(jsonPath("$.data.orderTotal").value(2000.0));
    }

    @Test
    @DisplayName("Franchise owner cannot set custom unit price or another franchise ID")
    void franchiseOwnerCannotSetUnitPriceOrAnotherFranchiseId() throws Exception {
        // Attempt 1: Franchise owner attempts to override unit price
        OrderCreateRequestDto priceTamperRequest = OrderCreateRequestDto.builder()
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(orangeProduct.getId())
                                .quantity(new BigDecimal("5.000"))
                                .unitPrice(new BigDecimal("100.00")) // Maliciously low unit price
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priceTamperRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Franchise owners are not permitted to set custom unit prices"));

        // Attempt 2: Franchise owner attempts to order on behalf of Franchise B
        OrderCreateRequestDto franchiseTamperRequest = OrderCreateRequestDto.builder()
                .franchiseId(franchiseB.getId()) // Owner A specifying Franchise B
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(orangeProduct.getId())
                                .quantity(new BigDecimal("5.000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(franchiseTamperRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Franchise owner cannot create orders for another franchise"));
    }

    @Test
    @DisplayName("Franchise owner cannot access another franchise's order by ID")
    void franchiseOwnerCannotAccessAnotherFranchiseOrder() throws Exception {
        // Create an order for Franchise B as Manager
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .franchiseId(franchiseB.getId())
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(mangoProduct.getId())
                                .quantity(new BigDecimal("3.000"))
                                .build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String orderBId = responseNode.path("data").path("id").asText();

        // Franchise Owner A attempts to access Franchise B's order
        mockMvc.perform(get("/api/v1/orders/" + orderBId)
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Franchise Owner B can access their own order
        mockMvc.perform(get("/api/v1/orders/" + orderBId)
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.franchiseId").value(franchiseB.getId().toString()));
    }

    @Test
    @DisplayName("Franchise owner retrieves only own franchise orders via /api/v1/my/orders")
    void franchiseOwnerRetrievesOnlyOwnOrders() throws Exception {
        // Create order for Franchise A
        OrderCreateRequestDto orderA = OrderCreateRequestDto.builder()
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(orangeProduct.getId())
                        .quantity(new BigDecimal("2.000"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderA)))
                .andExpect(status().isCreated());

        // Create order for Franchise B
        OrderCreateRequestDto orderB = OrderCreateRequestDto.builder()
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(lemonProduct.getId())
                        .quantity(new BigDecimal("3.000"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderB)))
                .andExpect(status().isCreated());

        // Owner A queries /api/v1/my/orders - should only see Franchise A's order
        mockMvc.perform(get("/api/v1/my/orders")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].franchiseId").value(franchiseA.getId().toString()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Full order lifecycle: DRAFT -> PLACED -> PREPARED -> DELIVERED with warehouse stock deduction")
    void orderLifecycleWithDeliveryStockDeduction() throws Exception {
        // Initial inventory: Inward 50.000 of orange and 50.000 of lemon
        inventoryService.increaseStock(
                orangeProduct,
                new BigDecimal("50.000"),
                StockMovementType.PURCHASE_IN,
                "PURCHASE_BILL",
                UUID.randomUUID(),
                "PUR-TEST-001",
                "Initial stock setup",
                managerUser.getId()
        );
        inventoryService.increaseStock(
                lemonProduct,
                new BigDecimal("50.000"),
                StockMovementType.PURCHASE_IN,
                "PURCHASE_BILL",
                UUID.randomUUID(),
                "PUR-TEST-001",
                "Initial stock setup",
                managerUser.getId()
        );

        // Step 1: Create order in DRAFT status
        OrderCreateRequestDto draftRequest = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.DRAFT)
                .notes("Draft order pending confirmation")
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(orangeProduct.getId())
                                .quantity(new BigDecimal("10.000"))
                                .build(),
                        OrderItemRequestDto.builder()
                                .productId(lemonProduct.getId())
                                .quantity(new BigDecimal("5.000"))
                                .build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        JsonNode orderNode = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        UUID orderId = UUID.fromString(orderNode.path("id").asText());
        String orderNumber = orderNode.path("orderNumber").asText();

        // Verify stock has NOT changed at DRAFT creation
        assertThat(inventoryService.getCurrentStock(orangeProduct.getId())).isEqualByComparingTo("50.000");
        assertThat(inventoryService.getCurrentStock(lemonProduct.getId())).isEqualByComparingTo("50.000");

        // Step 2: Move DRAFT to PLACED
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/place")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PLACED"));

        // Verify stock has NOT changed at PLACED
        assertThat(inventoryService.getCurrentStock(orangeProduct.getId())).isEqualByComparingTo("50.000");

        // Step 3: Move PLACED to PREPARED
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/prepare")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARED"))
                .andExpect(jsonPath("$.data.preparedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.preparedBy").value(managerUser.getId().toString()));

        // Verify stock has NOT changed at PREPARED
        assertThat(inventoryService.getCurrentStock(orangeProduct.getId())).isEqualByComparingTo("50.000");

        // Step 4: Move PREPARED to DELIVERED
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/deliver")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.deliveredAt").isNotEmpty())
                .andExpect(jsonPath("$.data.deliveredBy").value(managerUser.getId().toString()));

        // Verify stock has been accurately deducted
        // Orange: 50.000 - 10.000 = 40.000
        // Lemon: 50.000 - 5.000 = 45.000
        assertThat(inventoryService.getCurrentStock(orangeProduct.getId())).isEqualByComparingTo("40.000");
        assertThat(inventoryService.getCurrentStock(lemonProduct.getId())).isEqualByComparingTo("45.000");

        // Verify StockMovements created for DELIVERY_OUT
        List<StockMovement> movements = stockMovementRepository.findAll();
        List<StockMovement> deliveryMovements = movements.stream()
                .filter(m -> m.getMovementType() == StockMovementType.DELIVERY_OUT)
                .toList();

        assertThat(deliveryMovements).hasSize(2);
        for (StockMovement sm : deliveryMovements) {
            assertThat(sm.getReferenceType()).isEqualTo("SALES_ORDER");
            assertThat(sm.getReferenceId()).isEqualTo(orderId);
            assertThat(sm.getReferenceNumber()).isEqualTo(orderNumber);
            assertThat(sm.getCreatedBy()).isEqualTo(managerUser.getId());
        }
    }

    @Test
    @DisplayName("Insufficient warehouse stock prevents delivery with atomic rollback and no stock deduction")
    void insufficientStockPreventsDelivery() throws Exception {
        // Stock: Only 3.000 orange in stock, but order requires 5.000
        inventoryService.increaseStock(
                orangeProduct,
                new BigDecimal("3.000"),
                StockMovementType.PURCHASE_IN,
                "PURCHASE_BILL",
                UUID.randomUUID(),
                "PUR-TEST-002",
                "Low initial stock",
                managerUser.getId()
        );

        // Lemon has ample stock
        inventoryService.increaseStock(
                lemonProduct,
                new BigDecimal("50.000"),
                StockMovementType.PURCHASE_IN,
                "PURCHASE_BILL",
                UUID.randomUUID(),
                "PUR-TEST-002",
                "Ample stock",
                managerUser.getId()
        );

        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(orangeProduct.getId())
                                .quantity(new BigDecimal("5.000")) // Shortage! 3 available vs 5 required
                                .build(),
                        OrderItemRequestDto.builder()
                                .productId(lemonProduct.getId())
                                .quantity(new BigDecimal("2.000"))
                                .build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID orderId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Attempt delivery - should fail with 400 Bad Request detailing the shortage
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/deliver")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient warehouse stock for delivery")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Orange Syrup")));

        // Verify zero stock was deducted for any product (atomic rollback)
        assertThat(inventoryService.getCurrentStock(orangeProduct.getId())).isEqualByComparingTo("3.000");
        assertThat(inventoryService.getCurrentStock(lemonProduct.getId())).isEqualByComparingTo("50.000");

        // Verify order remains in PLACED status
        SalesOrder order = salesOrderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.PLACED);
    }

    @Test
    @DisplayName("Delivered order cannot be edited, cancelled, or deleted")
    void deliveredOrderCannotBeEditedCancelledOrDeleted() throws Exception {
        // Stock setup and order delivery
        inventoryService.increaseStock(orangeProduct, new BigDecimal("20.000"), StockMovementType.PURCHASE_IN, "REF", UUID.randomUUID(), "PUR-1", "Setup", managerUser.getId());

        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(orangeProduct.getId())
                        .quantity(new BigDecimal("5.000"))
                        .build()))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID orderId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Deliver
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/deliver")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        // Attempt 1: Edit delivered order -> 400 Bad Request
        OrderUpdateRequestDto updateDto = OrderUpdateRequestDto.builder()
                .notes("Trying to modify delivered order")
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(orangeProduct.getId())
                        .quantity(new BigDecimal("2.000"))
                        .build()))
                .build();

        mockMvc.perform(put("/api/v1/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Attempt 2: Cancel delivered order -> 400 Bad Request
        OrderCancelRequestDto cancelDto = OrderCancelRequestDto.builder()
                .reason("Cancel after delivery attempt")
                .build();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("DELIVERED orders cannot be cancelled"));

        // Attempt 3: Delete delivered order -> 400 Bad Request
        mockMvc.perform(delete("/api/v1/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only DRAFT orders can be deleted. Current status: DELIVERED"));
    }

    @Test
    @DisplayName("Only DRAFT orders can be deleted; PLACED orders cannot be deleted")
    void onlyDraftOrdersCanBeDeleted() throws Exception {
        // Create DRAFT order
        OrderCreateRequestDto draftDto = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .status(SalesOrderStatus.DRAFT)
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(orangeProduct.getId())
                        .quantity(new BigDecimal("2.000"))
                        .build()))
                .build();

        MvcResult draftResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftDto)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID draftId = UUID.fromString(objectMapper.readTree(draftResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Delete DRAFT order -> 200 OK
        mockMvc.perform(delete("/api/v1/orders/" + draftId)
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("DRAFT sales order deleted successfully"));

        assertThat(salesOrderRepository.findById(draftId)).isEmpty();

        // Create PLACED order
        OrderCreateRequestDto placedDto = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(orangeProduct.getId())
                        .quantity(new BigDecimal("2.000"))
                        .build()))
                .build();

        MvcResult placedResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(placedDto)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID placedId = UUID.fromString(objectMapper.readTree(placedResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Attempt to delete PLACED order -> 400 Bad Request
        mockMvc.perform(delete("/api/v1/orders/" + placedId)
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only DRAFT orders can be deleted. Current status: PLACED"));

        assertThat(salesOrderRepository.findById(placedId)).isPresent();
    }

    @Test
    @DisplayName("Cancellation requires reason and does not change stock")
    void cancellationRequiresReasonAndDoesNotChangeStock() throws Exception {
        inventoryService.increaseStock(orangeProduct, new BigDecimal("30.000"), StockMovementType.PURCHASE_IN, "REF", UUID.randomUUID(), "PUR-3", "Setup", managerUser.getId());

        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(orangeProduct.getId())
                        .quantity(new BigDecimal("5.000"))
                        .build()))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID orderId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Empty reason -> 400 Validation Error
        OrderCancelRequestDto emptyReasonDto = OrderCancelRequestDto.builder()
                .reason("")
                .build();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReasonDto)))
                .andExpect(status().isBadRequest());

        // Valid reason -> 200 OK
        OrderCancelRequestDto validReasonDto = OrderCancelRequestDto.builder()
                .reason("Customer called to cancel the order")
                .build();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReasonDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Customer called to cancel the order"))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());

        // Verify stock remains exactly 30.000 (no stock deduction occurred)
        assertThat(inventoryService.getCurrentStock(orangeProduct.getId())).isEqualByComparingTo("30.000");

        // Cannot cancel already CANCELLED order
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReasonDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Order is already CANCELLED"));
    }

    @Test
    @DisplayName("Only authorized roles can prepare and deliver; FRANCHISE_OWNER is forbidden")
    void onlyAuthorizedRolesCanPrepareAndDeliver() throws Exception {
        // Create order
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(orangeProduct.getId())
                        .quantity(new BigDecimal("2.000"))
                        .build()))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID orderId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Franchise owner attempts to PREPARE -> 403 Forbidden
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/prepare")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());

        // Franchise owner attempts to DELIVER -> 403 Forbidden
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/deliver")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());

        // Manager CAN prepare -> 200 OK
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/prepare")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARED"));
    }

    @Test
    @DisplayName("Transaction rollback works if one item stock deduction fails; zero partial deduction")
    void transactionRollbackWorksIfOneItemStockDeductionFails() throws Exception {
        // Orange has sufficient stock (10.000)
        inventoryService.increaseStock(orangeProduct, new BigDecimal("10.000"), StockMovementType.PURCHASE_IN, "REF", UUID.randomUUID(), "PUR-4", "Setup", managerUser.getId());
        // Mango has insufficient stock (1.000 vs 4.000 needed)
        inventoryService.increaseStock(mangoProduct, new BigDecimal("1.000"), StockMovementType.PURCHASE_IN, "REF", UUID.randomUUID(), "PUR-4", "Setup", managerUser.getId());

        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .productId(orangeProduct.getId())
                                .quantity(new BigDecimal("5.000"))
                                .build(),
                        OrderItemRequestDto.builder()
                                .productId(mangoProduct.getId())
                                .quantity(new BigDecimal("4.000"))
                                .build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID orderId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Attempt delivery -> Rejection due to Mango shortage
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/deliver")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Mango Syrup")));

        // Verify Orange stock is completely untouched (NO partial deduction)
        assertThat(inventoryService.getCurrentStock(orangeProduct.getId())).isEqualByComparingTo("10.000");
        assertThat(inventoryService.getCurrentStock(mangoProduct.getId())).isEqualByComparingTo("1.000");

        // Verify no DELIVERY_OUT movements exist
        List<StockMovement> deliveryMovements = stockMovementRepository.findAll().stream()
                .filter(m -> m.getMovementType() == StockMovementType.DELIVERY_OUT)
                .toList();
        assertThat(deliveryMovements).isEmpty();

        // Verify order is still in PLACED status
        SalesOrder order = salesOrderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(SalesOrderStatus.PLACED);
    }

    @Test
    @DisplayName("Manager can search, filter, and paginate orders")
    void managerCanSearchAndFilterOrders() throws Exception {
        // Create 2 orders for Franchise A
        OrderCreateRequestDto orderA1 = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .status(SalesOrderStatus.PLACED)
                .items(List.of(OrderItemRequestDto.builder().productId(orangeProduct.getId()).quantity(new BigDecimal("1.000")).build()))
                .build();
        OrderCreateRequestDto orderA2 = OrderCreateRequestDto.builder()
                .franchiseId(franchiseA.getId())
                .status(SalesOrderStatus.DRAFT)
                .items(List.of(OrderItemRequestDto.builder().productId(lemonProduct.getId()).quantity(new BigDecimal("2.000")).build()))
                .build();
        mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(orderA1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(orderA2))).andExpect(status().isCreated());

        // Filter by Franchise A -> 2 results
        mockMvc.perform(get("/api/v1/orders?franchiseId=" + franchiseA.getId())
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        // Filter by Status DRAFT -> 1 result
        mockMvc.perform(get("/api/v1/orders?status=DRAFT")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
    }
}
