package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceDueDateUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.PaymentCollectionRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Invoice;
import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.PaymentMode;
import com.asalkartechworks.daddysodahouse.entity.PaymentTerm;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.SalesOrder;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderItem;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.InventoryStockRepository;
import com.asalkartechworks.daddysodahouse.repository.InvoiceItemRepository;
import com.asalkartechworks.daddysodahouse.repository.InvoiceRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.repository.PaymentCollectionRepository;
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
import com.asalkartechworks.daddysodahouse.service.InvoiceService;
import com.asalkartechworks.daddysodahouse.util.TestDatabaseCleaner;
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

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvoicePaymentIntegrationTest {

    private final MockMvc mockMvc;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentCollectionRepository paymentCollectionRepository;
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
    private final InvoiceService invoiceService;
    private final TestDatabaseCleaner databaseCleaner;

    private Franchise franchiseA;
    private Franchise franchiseB;
    private User superAdminUser;
    private User managerUser;
    private User franchiseOwnerUserA;
    private User franchiseOwnerUserB;
    private Product orangeProduct;
    private Product lemonProduct;

    private String superAdminToken;
    private String managerToken;
    private String franchiseOwnerTokenA;
    private String franchiseOwnerTokenB;

    @Autowired
    public InvoicePaymentIntegrationTest(
            MockMvc mockMvc,
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            PaymentCollectionRepository paymentCollectionRepository,
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
            InventoryService inventoryService,
            InvoiceService invoiceService,
            TestDatabaseCleaner databaseCleaner) {
        this.mockMvc = mockMvc;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.paymentCollectionRepository = paymentCollectionRepository;
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
        this.invoiceService = invoiceService;
        this.databaseCleaner = databaseCleaner;
    }

    @BeforeEach
    void setUp() {
        databaseCleaner.cleanAll();

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

        superAdminToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(superAdminUser));
        managerToken = "Bearer " + jwtService.generateToken(UserPrincipal.create(managerUser));
        franchiseOwnerTokenA = "Bearer " + jwtService.generateToken(UserPrincipal.create(franchiseOwnerUserA));
        franchiseOwnerTokenB = "Bearer " + jwtService.generateToken(UserPrincipal.create(franchiseOwnerUserB));
    }

    private SalesOrder createDeliveredSalesOrder(Franchise franchise, BigDecimal orangeQty, BigDecimal orangePrice) {
        SalesOrder order = SalesOrder.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .franchise(franchise)
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.DELIVERED)
                .deliveredAt(Instant.now())
                .deliveredBy(managerUser.getId())
                .build();

        SalesOrderItem item = SalesOrderItem.builder()
                .salesOrder(order)
                .product(orangeProduct)
                .quantity(orangeQty)
                .unitPrice(orangePrice)
                .lineTotal(orangeQty.multiply(orangePrice).setScale(2, java.math.RoundingMode.HALF_UP))
                .build();

        order.addItem(item);
        return salesOrderRepository.save(order);
    }

    @Test
    @DisplayName("Invoice can be created ONLY for a DELIVERED sales order")
    void invoiceCanBeCreatedOnlyForDeliveredOrder() throws Exception {
        // 1. Create order in PLACED status
        SalesOrder placedOrder = SalesOrder.builder()
                .orderNumber("ORD-PLACED-001")
                .franchise(franchiseA)
                .orderDate(LocalDate.now())
                .status(SalesOrderStatus.PLACED)
                .build();
        placedOrder.addItem(SalesOrderItem.builder()
                .salesOrder(placedOrder)
                .product(orangeProduct)
                .quantity(new BigDecimal("5.000"))
                .unitPrice(new BigDecimal("500.00"))
                .lineTotal(new BigDecimal("2500.00"))
                .build());
        salesOrderRepository.save(placedOrder);

        InvoiceCreateRequestDto request = InvoiceCreateRequestDto.builder()
                .salesOrderId(placedOrder.getId())
                .invoiceType(InvoiceType.GST)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.CREDIT)
                .build();

        // Attempt creation for PLACED order -> 400 Bad Request
        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invoices can only be created for DELIVERED sales orders")));

        // 2. Mark order DELIVERED
        placedOrder.setStatus(SalesOrderStatus.DELIVERED);
        salesOrderRepository.save(placedOrder);

        // Attempt creation for DELIVERED order -> 201 Created
        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").value(org.hamcrest.Matchers.startsWith("GST-")));
    }

    @Test
    @DisplayName("One sales order can have ONLY ONE invoice")
    void oneInvoicePerSalesOrderConstraint() throws Exception {
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00"));

        InvoiceCreateRequestDto request = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.FULL_PAYMENT)
                .build();

        // First invoice -> 201 Created
        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate invoice attempt -> 400 Bad Request
        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("An invoice already exists for sales order")));
    }

    @Test
    @DisplayName("NORMAL invoice calculation has zero GST and uses INV- prefix")
    void normalInvoiceCalculationHasZeroGst() throws Exception {
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("4.000"), new BigDecimal("500.00")); // subtotal = 2000.00

        InvoiceCreateRequestDto request = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.FULL_PAYMENT)
                .notes("Retail normal bill")
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").value(org.hamcrest.Matchers.startsWith("INV-")))
                .andExpect(jsonPath("$.data.invoiceType").value("NORMAL"))
                .andExpect(jsonPath("$.data.subtotalAmount").value(2000.0))
                .andExpect(jsonPath("$.data.gstRatePercent").value(0.0))
                .andExpect(jsonPath("$.data.gstAmount").value(0.0))
                .andExpect(jsonPath("$.data.totalAmount").value(2000.0))
                .andExpect(jsonPath("$.data.paidAmount").value(0.0))
                .andExpect(jsonPath("$.data.balanceAmount").value(2000.0))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.dueDate").doesNotExist()); // FULL_PAYMENT has null dueDate
    }

    @Test
    @DisplayName("GST invoice calculation uses supplied or default 18% GST rate correctly")
    void gstInvoiceCalculation() throws Exception {
        // Subtotal = 10 * 500 = 5000.00
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("10.000"), new BigDecimal("500.00"));

        // Omit gstRatePercent -> defaults to 18.00%
        // 5000 * 18% = 900.00 GST, Total = 5900.00
        InvoiceCreateRequestDto defaultGstRequest = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.GST)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.CREDIT)
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGstRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber").value(org.hamcrest.Matchers.startsWith("GST-")))
                .andExpect(jsonPath("$.data.subtotalAmount").value(5000.0))
                .andExpect(jsonPath("$.data.gstRatePercent").value(18.0))
                .andExpect(jsonPath("$.data.gstAmount").value(900.0))
                .andExpect(jsonPath("$.data.totalAmount").value(5900.0))
                .andExpect(jsonPath("$.data.balanceAmount").value(5900.0));
    }

    @Test
    @DisplayName("CREDIT invoice automatically receives default due date = invoice date + 10 days")
    void creditInvoiceReceivesDefaultDueDate() throws Exception {
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00"));

        LocalDate today = LocalDate.now();
        InvoiceCreateRequestDto request = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(today)
                .paymentTerm(PaymentTerm.CREDIT) // Omit dueDate
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dueDate").value(today.plusDays(10).toString()));
    }

    @Test
    @DisplayName("Manager can update due date with mandatory reason; validation rejects invalid dates")
    void managerCanUpdateDueDateWithReason() throws Exception {
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00"));

        LocalDate today = LocalDate.now();
        InvoiceCreateRequestDto createReq = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(today)
                .paymentTerm(PaymentTerm.CREDIT)
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Update with missing reason -> 400 Bad Request
        InvoiceDueDateUpdateRequestDto invalidReq = InvoiceDueDateUpdateRequestDto.builder()
                .dueDate(today.plusDays(20))
                .reason("")
                .build();

        mockMvc.perform(patch("/api/v1/invoices/" + invoiceId + "/due-date")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());

        // Update with valid reason -> 200 OK
        LocalDate newDueDate = today.plusDays(25);
        InvoiceDueDateUpdateRequestDto validReq = InvoiceDueDateUpdateRequestDto.builder()
                .dueDate(newDueDate)
                .reason("Franchise requested credit extension for festival sale")
                .build();

        mockMvc.perform(patch("/api/v1/invoices/" + invoiceId + "/due-date")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dueDate").value(newDueDate.toString()))
                .andExpect(jsonPath("$.data.notes").value(org.hamcrest.Matchers.containsString("credit extension")));
    }

    @Test
    @DisplayName("Manual partial payment updates paid, balance, and status correctly; final payment marks PAID")
    void manualPaymentProgression() throws Exception {
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00")); // total = 1000.00

        InvoiceCreateRequestDto createReq = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.PARTIAL_PAYMENT)
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // 1. Partial payment: Rs 400.00
        PaymentCollectionRequestDto partialPayment = PaymentCollectionRequestDto.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("400.00"))
                .paymentMode(PaymentMode.UPI)
                .referenceNumber("UPI-4001")
                .remarks("First instalment")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paidAmount").value(400.0))
                .andExpect(jsonPath("$.data.balanceAmount").value(600.0))
                .andExpect(jsonPath("$.data.paymentStatus").value("PARTIALLY_PAID"));

        // 2. Overpayment attempt: Rs 650.00 (Balance is 600.00) -> 400 Bad Request
        PaymentCollectionRequestDto overPayment = PaymentCollectionRequestDto.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("650.00"))
                .paymentMode(PaymentMode.CASH)
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overPayment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("exceeds current invoice balance")));

        // 3. Final payment: Remaining Rs 600.00 -> status PAID
        PaymentCollectionRequestDto finalPayment = PaymentCollectionRequestDto.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("600.00"))
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .referenceNumber("NEFT-6002")
                .remarks("Final settlement")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paidAmount").value(1000.0))
                .andExpect(jsonPath("$.data.balanceAmount").value(0.0))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));

        // Verify payment history endpoint
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].amount").value(600.0))
                .andExpect(jsonPath("$.data[1].amount").value(400.0));
    }

    @Test
    @DisplayName("Overdue invoice is classified as OVERDUE after due date has passed")
    void overdueInvoiceClassification() throws Exception {
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00"));

        LocalDate pastDate = LocalDate.now().minusDays(20);
        LocalDate pastDueDate = LocalDate.now().minusDays(5);

        Invoice overdueInvoice = Invoice.builder()
                .salesOrder(deliveredOrder)
                .invoiceNumber("INV-PAST-001")
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(pastDate)
                .paymentTerm(PaymentTerm.CREDIT)
                .dueDate(pastDueDate)
                .subtotalAmount(new BigDecimal("1000.00"))
                .gstRatePercent(BigDecimal.ZERO)
                .gstAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(BigDecimal.ZERO)
                .balanceAmount(new BigDecimal("1000.00"))
                .paymentStatus(InvoicePaymentStatus.UNPAID)
                .build();

        Invoice saved = invoiceRepository.save(overdueInvoice);

        // Run overdue refresh
        invoiceService.refreshOverdueInvoices();

        Invoice refreshed = invoiceRepository.findById(saved.getId()).orElseThrow();
        assertThat(refreshed.getPaymentStatus()).isEqualTo(InvoicePaymentStatus.OVERDUE);
    }

    @Test
    @DisplayName("Franchise owner can access only own invoices and PDF; forbidden from other franchises")
    void franchiseOwnerMultiTenancyIsolation() throws Exception {
        // Invoice for Franchise B
        SalesOrder orderB = createDeliveredSalesOrder(franchiseB, new BigDecimal("2.000"), new BigDecimal("500.00"));
        InvoiceCreateRequestDto reqB = InvoiceCreateRequestDto.builder()
                .salesOrderId(orderB.getId())
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.FULL_PAYMENT)
                .build();

        MvcResult resB = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceBId = UUID.fromString(objectMapper.readTree(resB.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Invoice for Franchise A
        SalesOrder orderA = createDeliveredSalesOrder(franchiseA, new BigDecimal("3.000"), new BigDecimal("500.00"));
        InvoiceCreateRequestDto reqA = InvoiceCreateRequestDto.builder()
                .salesOrderId(orderA.getId())
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.FULL_PAYMENT)
                .build();

        MvcResult resA = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceAId = UUID.fromString(objectMapper.readTree(resA.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Owner A attempts to access Franchise B invoice detail -> 403 Forbidden
        mockMvc.perform(get("/api/v1/invoices/" + invoiceBId)
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());

        // Owner A attempts to download Franchise B PDF -> 403 Forbidden
        mockMvc.perform(get("/api/v1/invoices/" + invoiceBId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());

        // Owner A accesses OWN invoice detail -> 200 OK
        mockMvc.perform(get("/api/v1/invoices/" + invoiceAId)
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.franchiseId").value(franchiseA.getId().toString()));

        // Owner A queries /api/v1/my/invoices -> exactly 1 invoice
        mockMvc.perform(get("/api/v1/my/invoices")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].franchiseId").value(franchiseA.getId().toString()));
    }

    @Test
    @DisplayName("Franchise owner is forbidden from creating invoices, recording payments, or updating due dates")
    void franchiseOwnerForbiddenFromMutations() throws Exception {
        SalesOrder orderA = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00"));

        InvoiceCreateRequestDto createReq = InvoiceCreateRequestDto.builder()
                .salesOrderId(orderA.getId())
                .invoiceType(InvoiceType.NORMAL)
                .paymentTerm(PaymentTerm.FULL_PAYMENT)
                .build();

        // Owner creates invoice -> 403 Forbidden
        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // Create invoice as Manager
        MvcResult res = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Owner records payment -> 403 Forbidden
        PaymentCollectionRequestDto payReq = PaymentCollectionRequestDto.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("100.00"))
                .paymentMode(PaymentMode.CASH)
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isForbidden());

        // Owner updates due date -> 403 Forbidden
        InvoiceDueDateUpdateRequestDto dueReq = InvoiceDueDateUpdateRequestDto.builder()
                .dueDate(LocalDate.now().plusDays(30))
                .reason("Attempted owner update")
                .build();

        mockMvc.perform(patch("/api/v1/invoices/" + invoiceId + "/due-date")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dueReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PDF is generated and downloadable; missing PDF is regenerated automatically")
    void pdfGenerationAndDownloadWithRegeneration() throws Exception {
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("3.000"), new BigDecimal("500.00"));

        InvoiceCreateRequestDto req = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.GST)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.CREDIT)
                .notes("Check PDF generator")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // 1. Download PDF -> 200 OK, application/pdf
        MvcResult downloadResult = mockMvc.perform(get("/api/v1/invoices/" + invoiceId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] pdfBytes = downloadResult.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        // PDF magic header %PDF-
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        // 2. Delete the PDF file from disk to test dynamic regeneration
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        if (invoice.getPdfFilePath() != null) {
            File pdfFile = new File(invoice.getPdfFilePath());
            if (pdfFile.exists()) {
                boolean deleted = pdfFile.delete();
                assertThat(deleted).isTrue();
            }
        }

        // 3. Download again -> Should regenerate and return 200 OK with valid PDF bytes
        MvcResult regenResult = mockMvc.perform(get("/api/v1/invoices/" + invoiceId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] regenBytes = regenResult.getResponse().getContentAsByteArray();
        assertThat(regenBytes).isNotEmpty();
        assertThat(new String(regenBytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Invoice item snapshot remains unchanged if product master price changes later")
    void invoiceItemSnapshotImmutability() throws Exception {
        // Delivered order invoiced at 500.00
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00"));

        InvoiceCreateRequestDto req = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.NORMAL)
                .paymentTerm(PaymentTerm.FULL_PAYMENT)
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Update product master price from 500.00 to 800.00
        orangeProduct.setDefaultSellingPrice(new BigDecimal("800.00"));
        productRepository.save(orangeProduct);

        // Fetch invoice details -> Items must still retain original snapshot price 500.00
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId)
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(500.0))
                .andExpect(jsonPath("$.data.items[0].lineTotal").value(1000.0))
                .andExpect(jsonPath("$.data.totalAmount").value(1000.0));
    }

    @Test
    @DisplayName("GST invoice calculation supports explicit custom GST percentage")
    void customGstRateCalculation() throws Exception {
        // Subtotal = 2 * 500 = 1000.00
        SalesOrder deliveredOrder = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00"));

        // Explicit 12.00% GST: 1000 * 12% = 120.00, total = 1120.00
        InvoiceCreateRequestDto customGstReq = InvoiceCreateRequestDto.builder()
                .salesOrderId(deliveredOrder.getId())
                .invoiceType(InvoiceType.GST)
                .invoiceDate(LocalDate.now())
                .paymentTerm(PaymentTerm.CREDIT)
                .gstRatePercent(new BigDecimal("12.00"))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customGstReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gstRatePercent").value(12.0))
                .andExpect(jsonPath("$.data.gstAmount").value(120.0))
                .andExpect(jsonPath("$.data.totalAmount").value(1120.0));
    }

    @Test
    @DisplayName("Manager can search, filter, and paginate invoices")
    void managerCanSearchAndFilterInvoices() throws Exception {
        SalesOrder order1 = createDeliveredSalesOrder(franchiseA, new BigDecimal("1.000"), new BigDecimal("500.00"));
        SalesOrder order2 = createDeliveredSalesOrder(franchiseA, new BigDecimal("2.000"), new BigDecimal("500.00"));

        // Create 1 GST and 1 NORMAL invoice
        InvoiceCreateRequestDto req1 = InvoiceCreateRequestDto.builder()
                .salesOrderId(order1.getId())
                .invoiceType(InvoiceType.GST)
                .paymentTerm(PaymentTerm.CREDIT)
                .build();
        InvoiceCreateRequestDto req2 = InvoiceCreateRequestDto.builder()
                .salesOrderId(order2.getId())
                .invoiceType(InvoiceType.NORMAL)
                .paymentTerm(PaymentTerm.FULL_PAYMENT)
                .build();

        mockMvc.perform(post("/api/v1/invoices").header(HttpHeaders.AUTHORIZATION, managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/invoices").header(HttpHeaders.AUTHORIZATION, managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        // Filter by invoiceType = GST -> exactly 1
        mockMvc.perform(get("/api/v1/invoices?invoiceType=GST")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].invoiceType").value("GST"));

        // Filter by franchise A -> 2
        mockMvc.perform(get("/api/v1/invoices?franchiseId=" + franchiseA.getId())
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }
}

