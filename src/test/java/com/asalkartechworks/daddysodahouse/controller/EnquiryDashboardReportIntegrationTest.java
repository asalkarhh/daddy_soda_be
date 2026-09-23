package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.enquiry.EnquiryStatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.enquiry.PublicEnquiryRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Enquiry;
import com.asalkartechworks.daddysodahouse.entity.EnquiryStatus;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Invoice;
import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.Machine;
import com.asalkartechworks.daddysodahouse.entity.Outlet;
import com.asalkartechworks.daddysodahouse.entity.PaymentTerm;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.PurchaseEntry;
import com.asalkartechworks.daddysodahouse.entity.PurchaseItem;
import com.asalkartechworks.daddysodahouse.entity.PurchaseStatus;
import com.asalkartechworks.daddysodahouse.entity.SalesOrder;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderItem;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.entity.Supplier;
import com.asalkartechworks.daddysodahouse.entity.User;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.repository.EnquiryRepository;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.InvoiceRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseEntryRepository;
import com.asalkartechworks.daddysodahouse.repository.SalesOrderRepository;
import com.asalkartechworks.daddysodahouse.repository.SupplierRepository;
import com.asalkartechworks.daddysodahouse.repository.UserRepository;
import com.asalkartechworks.daddysodahouse.security.JwtService;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.util.CsvExportUtil;
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

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnquiryDashboardReportIntegrationTest {

    private final MockMvc mockMvc;
    private final EnquiryRepository enquiryRepository;
    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PurchaseEntryRepository purchaseEntryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final FranchiseRepository franchiseRepository;
    private final OutletRepository outletRepository;
    private final MachineRepository machineRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final TestDatabaseCleaner databaseCleaner;

    private Franchise franchiseA;
    private Franchise franchiseB;
    private Outlet outletA;
    private Machine machineA;
    private User superAdminUser;
    private User managerUser;
    private User franchiseOwnerUserA;
    private User franchiseOwnerUserB;
    private Product orangeProduct;
    private Supplier testSupplier;

    private String superAdminToken;
    private String managerToken;
    private String franchiseOwnerTokenA;
    private String franchiseOwnerTokenB;

    @Autowired
    public EnquiryDashboardReportIntegrationTest(
            MockMvc mockMvc,
            EnquiryRepository enquiryRepository,
            InvoiceRepository invoiceRepository,
            SalesOrderRepository salesOrderRepository,
            PurchaseEntryRepository purchaseEntryRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            FranchiseRepository franchiseRepository,
            OutletRepository outletRepository,
            MachineRepository machineRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ObjectMapper objectMapper,
            TestDatabaseCleaner databaseCleaner) {
        this.mockMvc = mockMvc;
        this.enquiryRepository = enquiryRepository;
        this.invoiceRepository = invoiceRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.purchaseEntryRepository = purchaseEntryRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.franchiseRepository = franchiseRepository;
        this.outletRepository = outletRepository;
        this.machineRepository = machineRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
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

        outletA = outletRepository.save(Outlet.builder()
                .franchise(franchiseA)
                .outletCode("OUT-001")
                .outletName("Pune Station Counter")
                .city("Pune")
                .state("Maharashtra")
                .isActive(true)
                .build());

        machineA = machineRepository.save(Machine.builder()
                .outlet(outletA)
                .machineCode("MAC-001")
                .machineName("Soda Unit 1")
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

        testSupplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-TEST-01")
                .supplierName("Apex Flavour Ingredients")
                .mobile("9822001122")
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
                .email("rohan@franchisea.com")
                .passwordHash(passwordEncoder.encode("Owner@12345"))
                .role(UserRole.FRANCHISE_OWNER)
                .franchise(franchiseA)
                .isActive(true)
                .build());

        franchiseOwnerUserB = userRepository.save(User.builder()
                .fullName("Suresh Deshmukh")
                .mobile("9111100002")
                .email("suresh@franchiseb.com")
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

    @Test
    @DisplayName("Public enquiry is saved and returns valid WhatsApp redirect URL")
    void publicEnquiry_savesAndReturnsValidWhatsAppUrl() throws Exception {
        PublicEnquiryRequestDto requestDto = PublicEnquiryRequestDto.builder()
                .fullName("Amit Patil")
                .mobile("9876543210")
                .email("amit@example.com")
                .city("Pune")
                .message("I want details about soda franchise opportunity.")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/public/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Enquiry submitted successfully")))
                .andExpect(jsonPath("$.data.enquiryNumber").exists())
                .andExpect(jsonPath("$.data.whatsAppUrl").exists())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String enquiryNumber = root.path("data").path("enquiryNumber").asText();
        String whatsAppUrl = root.path("data").path("whatsAppUrl").asText();

        assertThat(enquiryNumber).startsWith("ENQ-");
        assertThat(whatsAppUrl).startsWith("https://wa.me/919876543210?text=");

        // Verify prefilled content
        String queryText = URLDecoder.decode(whatsAppUrl.substring(whatsAppUrl.indexOf("text=") + 5), StandardCharsets.UTF_8);
        assertThat(queryText).contains("Website Franchise Enquiry");
        assertThat(queryText).contains("Amit Patil");
        assertThat(queryText).contains("9876543210");
        assertThat(queryText).contains("amit@example.com");
        assertThat(queryText).contains("Pune");
        assertThat(queryText).contains("I want details about soda franchise opportunity.");

        // Verify stored entity
        Enquiry saved = enquiryRepository.findByEnquiryNumber(enquiryNumber).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EnquiryStatus.NEW);
        assertThat(saved.getSource()).isEqualTo("WEBSITE");
    }

    @Test
    @DisplayName("Public enquiry rejects invalid mobile number")
    void publicEnquiry_invalidMobile_rejected() throws Exception {
        PublicEnquiryRequestDto requestDto = PublicEnquiryRequestDto.builder()
                .fullName("Amit Patil")
                .mobile("98765") // Only 5 digits
                .build();

        mockMvc.perform(post("/api/v1/public/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Enquiry status update rules: NEW -> CONTACTED -> CLOSED; direct NEW -> CLOSED rejected for Manager")
    void enquiryManagement_statusTransitions() throws Exception {
        Enquiry enquiry = enquiryRepository.save(Enquiry.builder()
                .enquiryNumber("ENQ-202609-00001")
                .fullName("Rajesh Sharma")
                .mobile("9822114455")
                .status(EnquiryStatus.NEW)
                .build());

        // Manager tries to close NEW enquiry directly -> 400 Bad Request
        EnquiryStatusUpdateRequestDto directCloseDto = EnquiryStatusUpdateRequestDto.builder()
                .status(EnquiryStatus.CLOSED)
                .adminNotes("Close without contacting")
                .build();

        mockMvc.perform(patch("/api/v1/enquiries/" + enquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(directCloseDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("transition through CONTACTED first")));

        // Manager moves NEW -> CONTACTED -> 200 OK and contacted_at is populated
        EnquiryStatusUpdateRequestDto contactDto = EnquiryStatusUpdateRequestDto.builder()
                .status(EnquiryStatus.CONTACTED)
                .adminNotes("Called and discussed shop requirements.")
                .build();

        mockMvc.perform(patch("/api/v1/enquiries/" + enquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CONTACTED")))
                .andExpect(jsonPath("$.data.contactedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.adminNotes", is("Called and discussed shop requirements.")));

        // Manager moves CONTACTED -> CLOSED -> 200 OK
        EnquiryStatusUpdateRequestDto closeDto = EnquiryStatusUpdateRequestDto.builder()
                .status(EnquiryStatus.CLOSED)
                .adminNotes("Agreement signed.")
                .build();

        mockMvc.perform(patch("/api/v1/enquiries/" + enquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CLOSED")));

        // Super Admin can close directly from NEW
        Enquiry enquiry2 = enquiryRepository.save(Enquiry.builder()
                .enquiryNumber("ENQ-202609-00002")
                .fullName("Direct Admin Applicant")
                .mobile("9822114466")
                .status(EnquiryStatus.NEW)
                .build());

        mockMvc.perform(patch("/api/v1/enquiries/" + enquiry2.getId())
                        .header(HttpHeaders.AUTHORIZATION, superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(directCloseDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CLOSED")));
    }

    @Test
    @DisplayName("Dashboard due classification: OVERDUE (RED), DUE_TODAY (AMBER), UPCOMING (NORMAL)")
    void dashboardDues_classification_and_severity() throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Overdue Invoice (Due 5 days ago)
        createTestInvoice(franchiseA, "ORD-TEST-001", "INV-202609-00001",
                today.minusDays(15), today.minusDays(5), new BigDecimal("1000.00"), new BigDecimal("1000.00"), InvoicePaymentStatus.OVERDUE);

        // 2. Due Today Invoice
        createTestInvoice(franchiseA, "ORD-TEST-002", "INV-202609-00002",
                today.minusDays(10), today, new BigDecimal("800.00"), new BigDecimal("800.00"), InvoicePaymentStatus.UNPAID);

        // 3. Upcoming Invoice (Due 10 days later)
        createTestInvoice(franchiseB, "ORD-TEST-003", "INV-202609-00003",
                today, today.plusDays(10), new BigDecimal("600.00"), new BigDecimal("600.00"), InvoicePaymentStatus.UNPAID);

        // Manager lists all dues
        mockMvc.perform(get("/api/v1/dashboard/dues")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)));

        // Filter only OVERDUE dues
        mockMvc.perform(get("/api/v1/dashboard/dues")
                        .param("dueType", "OVERDUE")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber", is("INV-202609-00001")))
                .andExpect(jsonPath("$.data.content[0].dueType", is("OVERDUE")))
                .andExpect(jsonPath("$.data.content[0].displaySeverity", is("RED")))
                .andExpect(jsonPath("$.data.content[0].daysOverdue", is(5)));

        // Filter only DUE_TODAY dues
        mockMvc.perform(get("/api/v1/dashboard/dues")
                        .param("dueType", "DUE_TODAY")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber", is("INV-202609-00002")))
                .andExpect(jsonPath("$.data.content[0].dueType", is("DUE_TODAY")))
                .andExpect(jsonPath("$.data.content[0].displaySeverity", is("AMBER")))
                .andExpect(jsonPath("$.data.content[0].daysOverdue", is(0)));

        // Filter only UPCOMING dues
        mockMvc.perform(get("/api/v1/dashboard/dues")
                        .param("dueType", "UPCOMING")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber", is("INV-202609-00003")))
                .andExpect(jsonPath("$.data.content[0].dueType", is("UPCOMING")))
                .andExpect(jsonPath("$.data.content[0].displaySeverity", is("NORMAL")))
                .andExpect(jsonPath("$.data.content[0].daysOverdue", is(0)));
    }

    @Test
    @DisplayName("Franchise owner sees only their own dashboard dues")
    void dashboardDues_franchiseOwner_isolation() throws Exception {
        LocalDate today = LocalDate.now();

        // Franchise A invoice
        createTestInvoice(franchiseA, "ORD-A-001", "INV-A-001",
                today.minusDays(10), today.minusDays(2), new BigDecimal("1000.00"), new BigDecimal("1000.00"), InvoicePaymentStatus.OVERDUE);

        // Franchise B invoice
        createTestInvoice(franchiseB, "ORD-B-001", "INV-B-001",
                today.minusDays(10), today.minusDays(2), new BigDecimal("2000.00"), new BigDecimal("2000.00"), InvoicePaymentStatus.OVERDUE);

        // Franchise Owner A queries dues
        mockMvc.perform(get("/api/v1/dashboard/dues")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber", is("INV-A-001")))
                .andExpect(jsonPath("$.data.content[0].franchiseName", is("Franchise Pune Station")));

        // Franchise Owner B queries dues
        mockMvc.perform(get("/api/v1/dashboard/dues")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber", is("INV-B-001")))
                .andExpect(jsonPath("$.data.content[0].franchiseName", is("Franchise Kothrud")));
    }

    @Test
    @DisplayName("Operational dashboard summary aggregates operational numbers accurately")
    void dashboardOperationalSummary_manager_success() throws Exception {
        LocalDate today = LocalDate.now();

        // 1 enquiry
        enquiryRepository.save(Enquiry.builder()
                .enquiryNumber("ENQ-202609-00001")
                .fullName("Prospective Owner")
                .mobile("9876543210")
                .status(EnquiryStatus.NEW)
                .build());

        // 1 invoice with overdue balance
        createTestInvoice(franchiseA, "ORD-SUM-001", "INV-SUM-001",
                today.minusDays(12), today.minusDays(2), new BigDecimal("1500.00"), new BigDecimal("1500.00"), InvoicePaymentStatus.OVERDUE);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeFranchises", is(2)))
                .andExpect(jsonPath("$.data.activeOutlets", is(1)))
                .andExpect(jsonPath("$.data.activeMachines", is(1)))
                .andExpect(jsonPath("$.data.activeProducts", is(1)))
                .andExpect(jsonPath("$.data.newEnquiryCount", is(1)))
                .andExpect(jsonPath("$.data.pendingInvoiceCount", is(1)))
                .andExpect(jsonPath("$.data.overdueInvoiceCount", is(1)))
                .andExpect(jsonPath("$.data.overdueAmount", is(1500.00)));
    }

    @Test
    @DisplayName("Franchise Owner summary provides personalized counts, orders by status, and dues")
    void dashboardFranchiseSummary_franchiseOwner_success() throws Exception {
        LocalDate today = LocalDate.now();

        createTestInvoice(franchiseA, "ORD-FO-001", "INV-FO-001",
                today.minusDays(10), today.plusDays(5), new BigDecimal("1200.00"), new BigDecimal("1200.00"), InvoicePaymentStatus.UNPAID);

        mockMvc.perform(get("/api/v1/dashboard/franchise-summary")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.franchiseName", is("Franchise Pune Station")))
                .andExpect(jsonPath("$.data.activeOutletCount", is(1)))
                .andExpect(jsonPath("$.data.machineCount", is(1)))
                .andExpect(jsonPath("$.data.ordersByStatus").isMap())
                .andExpect(jsonPath("$.data.nextDueInvoice.invoiceNumber", is("INV-FO-001")))
                .andExpect(jsonPath("$.data.nextDueInvoice.balanceAmount", is(1200.00)))
                .andExpect(jsonPath("$.data.latestFiveInvoices", hasSize(1)));
    }

    @Test
    @DisplayName("Monthly purchases report generates and downloads valid CSV")
    void monthlyReports_purchasesDownload_csv() throws Exception {
        LocalDate today = LocalDate.now();
        String currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        PurchaseEntry entry = PurchaseEntry.builder()
                .purchaseNumber("PUR-202609-00001")
                .supplier(testSupplier)
                .supplierBillNumber("BILL-7788")
                .billDate(today)
                .status(PurchaseStatus.CONFIRMED)
                .description("Restock flavors")
                .build();
        entry.addItem(PurchaseItem.builder()
                .product(orangeProduct)
                .quantity(new BigDecimal("10.000"))
                .build());
        purchaseEntryRepository.save(entry);

        MvcResult result = mockMvc.perform(get("/api/v1/reports/monthly/purchases/download")
                        .param("month", currentMonth)
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"monthly-purchases-" + currentMonth + ".csv\""))
                .andReturn();

        String csv = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).contains("Purchase Number,Bill Date,Supplier,Supplier Bill Number");
        assertThat(csv).contains("PUR-202609-00001");
        assertThat(csv).contains("Apex Flavour Ingredients");
        assertThat(csv).contains("BILL-7788");
        assertThat(csv).contains("SYR-ORG-05L");
        assertThat(csv).contains("10.000");
    }

    @Test
    @DisplayName("Monthly orders report restricts Franchise Owner to own franchise")
    void monthlyReports_ordersDownload_franchiseOwner_isolation() throws Exception {
        LocalDate today = LocalDate.now();
        String currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Order for Franchise A
        SalesOrder orderA = SalesOrder.builder()
                .orderNumber("ORD-CSV-A")
                .franchise(franchiseA)
                .orderDate(today)
                .status(SalesOrderStatus.DELIVERED)
                .build();
        orderA.addItem(SalesOrderItem.builder().product(orangeProduct).quantity(new BigDecimal("2.000")).unitPrice(new BigDecimal("500.00")).lineTotal(new BigDecimal("1000.00")).build());
        salesOrderRepository.save(orderA);

        // Order for Franchise B
        SalesOrder orderB = SalesOrder.builder()
                .orderNumber("ORD-CSV-B")
                .franchise(franchiseB)
                .orderDate(today)
                .status(SalesOrderStatus.DELIVERED)
                .build();
        orderB.addItem(SalesOrderItem.builder().product(orangeProduct).quantity(new BigDecimal("5.000")).unitPrice(new BigDecimal("500.00")).lineTotal(new BigDecimal("2500.00")).build());
        salesOrderRepository.save(orderB);

        // Franchise Owner A downloads report -> should contain ORD-CSV-A and NEVER ORD-CSV-B
        MvcResult result = mockMvc.perform(get("/api/v1/reports/monthly/orders/download")
                        .param("month", currentMonth)
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isOk())
                .andReturn();

        String csv = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).contains("ORD-CSV-A");
        assertThat(csv).contains("Franchise Pune Station");
        assertThat(csv).doesNotContain("ORD-CSV-B");
        assertThat(csv).doesNotContain("Franchise Kothrud");
    }

    @Test
    @DisplayName("Monthly reports reject invalid month parameter format")
    void monthlyReports_invalidMonthFormat_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/reports/monthly/purchases/download")
                        .param("month", "2026-13") // invalid month 13
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid month format")));

        mockMvc.perform(get("/api/v1/reports/monthly/purchases/download")
                        .param("month", "invalid-month")
                        .header(HttpHeaders.AUTHORIZATION, managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("CSV utility sanitizes spreadsheet formula injection characters")
    void csvFormulaInjection_escaping() {
        assertThat(CsvExportUtil.escapeCell("=SUM(A1:A10)")).isEqualTo("'=SUM(A1:A10)");
        assertThat(CsvExportUtil.escapeCell("=SUM(A1,A10)")).isEqualTo("\"'=SUM(A1,A10)\"");
        assertThat(CsvExportUtil.escapeCell("+12345")).isEqualTo("'+12345");
        assertThat(CsvExportUtil.escapeCell("-50.00")).isEqualTo("'-50.00");
        assertThat(CsvExportUtil.escapeCell("@cmd")).isEqualTo("'@cmd");
        assertThat(CsvExportUtil.escapeCell("Regular, with comma")).isEqualTo("\"Regular, with comma\"");
        assertThat(CsvExportUtil.escapeCell("With \"quotes\"")).isEqualTo("\"With \"\"quotes\"\"\"");
    }

    @Test
    @DisplayName("Franchise Owner is blocked from managing enquiries and viewing operational summary")
    void roleRestrictions_franchiseOwner_blockedFromEnquiriesAndOperationalSummary() throws Exception {
        mockMvc.perform(get("/api/v1/enquiries")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/reports/monthly/purchases/download")
                        .param("month", "2026-09")
                        .header(HttpHeaders.AUTHORIZATION, franchiseOwnerTokenA))
                .andExpect(status().isForbidden());
    }

    private Invoice createTestInvoice(
            Franchise franchise,
            String orderNumber,
            String invoiceNumber,
            LocalDate invoiceDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            BigDecimal balanceAmount,
            InvoicePaymentStatus paymentStatus) {

        SalesOrder order = salesOrderRepository.save(SalesOrder.builder()
                .franchise(franchise)
                .orderNumber(orderNumber)
                .orderDate(invoiceDate)
                .status(SalesOrderStatus.DELIVERED)
                .build());

        Invoice invoice = Invoice.builder()
                .salesOrder(order)
                .invoiceNumber(invoiceNumber)
                .invoiceType(InvoiceType.NORMAL)
                .invoiceDate(invoiceDate)
                .paymentTerm(PaymentTerm.CREDIT)
                .dueDate(dueDate)
                .subtotalAmount(totalAmount)
                .gstRatePercent(BigDecimal.ZERO)
                .gstAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .paidAmount(totalAmount.subtract(balanceAmount))
                .balanceAmount(balanceAmount)
                .paymentStatus(paymentStatus)
                .build();

        return invoiceRepository.save(invoice);
    }
}
