package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.dashboard.DashboardSummaryResponseDto;
import com.asalkartechworks.daddysodahouse.dto.dashboard.DueInvoiceResponseDto;
import com.asalkartechworks.daddysodahouse.dto.dashboard.DueSeverity;
import com.asalkartechworks.daddysodahouse.dto.dashboard.DueType;
import com.asalkartechworks.daddysodahouse.dto.dashboard.FranchiseDashboardSummaryResponseDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderSummaryDto;
import com.asalkartechworks.daddysodahouse.entity.EnquiryStatus;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Invoice;
import com.asalkartechworks.daddysodahouse.entity.PurchaseStatus;
import com.asalkartechworks.daddysodahouse.entity.SalesOrder;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.asalkartechworks.daddysodahouse.repository.EnquiryRepository;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.InventoryStockRepository;
import com.asalkartechworks.daddysodahouse.repository.InvoiceRepository;
import com.asalkartechworks.daddysodahouse.repository.MachineRepository;
import com.asalkartechworks.daddysodahouse.repository.OutletRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseEntryRepository;
import com.asalkartechworks.daddysodahouse.repository.SalesOrderRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service aggregating business operations metrics, credit dues list with color indicators,
 * and personalized franchise summaries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FranchiseRepository franchiseRepository;
    private final OutletRepository outletRepository;
    private final MachineRepository machineRepository;
    private final ProductRepository productRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final PurchaseEntryRepository purchaseEntryRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final EnquiryRepository enquiryRepository;

    /**
     * Complete operational dashboard summary for SUPER_ADMIN and MANAGER.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponseDto getOperationalSummary() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        long activeFranchises = franchiseRepository.countByIsActiveTrue();
        long activeOutlets = outletRepository.countByIsActiveTrue();
        long activeMachines = machineRepository.countByIsActiveTrue();
        long activeProducts = productRepository.countByIsActiveTrue();

        long inStock = inventoryStockRepository.countActiveProductsInStock();
        long outOfStock = inventoryStockRepository.countActiveProductsOutOfStock();

        long purchasesCount = purchaseEntryRepository.countByBillDateBetweenAndStatus(
                startOfMonth, endOfMonth, PurchaseStatus.CONFIRMED);
        BigDecimal purchaseQty = purchaseEntryRepository.sumQuantityByBillDateBetweenAndStatus(
                startOfMonth, endOfMonth, PurchaseStatus.CONFIRMED);

        long ordersPlaced = salesOrderRepository.countByStatus(SalesOrderStatus.PLACED);
        long ordersPrepared = salesOrderRepository.countByStatus(SalesOrderStatus.PREPARED);
        long ordersDelivered = salesOrderRepository.countByStatusAndOrderDateBetween(
                SalesOrderStatus.DELIVERED, startOfMonth, endOfMonth);

        long invoicesMonthCount = invoiceRepository.countByInvoiceDateBetween(startOfMonth, endOfMonth);
        BigDecimal invoiceMonthTotal = invoiceRepository.sumTotalAmountByInvoiceDateBetween(startOfMonth, endOfMonth);

        long pendingInvoices = invoiceRepository.countPendingInvoices();
        BigDecimal pendingAmount = invoiceRepository.sumPendingAmount();

        long overdueInvoices = invoiceRepository.countOverdueInvoices(today);
        BigDecimal overdueAmount = invoiceRepository.sumOverdueAmount(today);

        long newEnquiries = enquiryRepository.countByStatus(EnquiryStatus.NEW);

        return DashboardSummaryResponseDto.builder()
                .activeFranchises(activeFranchises)
                .activeOutlets(activeOutlets)
                .activeMachines(activeMachines)
                .activeProducts(activeProducts)
                .warehouseProductsInStock(inStock)
                .warehouseProductsOutOfStock(outOfStock)
                .purchasesThisMonthCount(purchasesCount)
                .purchaseQuantityThisMonth(purchaseQty)
                .ordersPlacedCount(ordersPlaced)
                .ordersPreparedCount(ordersPrepared)
                .ordersDeliveredThisMonthCount(ordersDelivered)
                .invoicesThisMonthCount(invoicesMonthCount)
                .invoiceTotalThisMonth(invoiceMonthTotal)
                .pendingInvoiceCount(pendingInvoices)
                .pendingAmount(pendingAmount)
                .overdueInvoiceCount(overdueInvoices)
                .overdueAmount(overdueAmount)
                .newEnquiryCount(newEnquiries)
                .build();
    }

    /**
     * Query credit due invoices with visual severity tags.
     * Accessible by SUPER_ADMIN, MANAGER (all dues) and FRANCHISE_OWNER (scoped to own franchise).
     */
    @Transactional(readOnly = true)
    public Page<DueInvoiceResponseDto> getDues(
            DueType dueType,
            LocalDate fromDate,
            LocalDate toDate,
            UUID requestedFranchiseId,
            Pageable pageable,
            UserPrincipal principal) {

        UUID franchiseId = requestedFranchiseId;
        if (principal != null && principal.getRole() == UserRole.FRANCHISE_OWNER) {
            franchiseId = principal.getFranchiseId();
            if (franchiseId == null) {
                throw new AccessDeniedException("Franchise owner is not associated with any franchise");
            }
        }

        LocalDate today = LocalDate.now();
        String dueTypeStr = dueType != null ? dueType.name() : null;

        Page<Invoice> invoices = invoiceRepository.findDues(franchiseId, fromDate, toDate, dueTypeStr, today, pageable);

        return invoices.map(inv -> toDueDto(inv, today));
    }

    /**
     * Personalized franchise summary for the authenticated franchise owner.
     */
    @Transactional(readOnly = true)
    public FranchiseDashboardSummaryResponseDto getFranchiseSummary(UserPrincipal principal) {
        if (principal == null || principal.getRole() != UserRole.FRANCHISE_OWNER || principal.getFranchiseId() == null) {
            throw new AccessDeniedException("Access restricted to authenticated franchise owners");
        }

        UUID franchiseId = principal.getFranchiseId();
        Franchise franchise = franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", franchiseId));

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        long activeOutlets = outletRepository.countByFranchiseIdAndIsActiveTrue(franchiseId);
        long machineCount = machineRepository.countByOutletFranchiseId(franchiseId);

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (SalesOrderStatus status : SalesOrderStatus.values()) {
            ordersByStatus.put(status.name(), salesOrderRepository.countByFranchiseIdAndStatus(franchiseId, status));
        }

        BigDecimal invoiceTotalMonth = invoiceRepository.sumTotalAmountByFranchiseIdAndInvoiceDateBetween(
                franchiseId, startOfMonth, endOfMonth);
        BigDecimal pendingAmount = invoiceRepository.sumPendingAmountByFranchiseId(franchiseId);
        BigDecimal overdueAmount = invoiceRepository.sumOverdueAmountByFranchiseId(franchiseId, today);
        long overdueCount = invoiceRepository.countOverdueInvoicesByFranchiseId(franchiseId, today);

        List<Invoice> topDues = invoiceRepository.findTopDueInvoicesByFranchiseId(franchiseId, PageRequest.of(0, 1));
        DueInvoiceResponseDto nextDueInvoice = topDues.isEmpty() ? null : toDueDto(topDues.get(0), today);

        List<OrderSummaryDto> latestOrders = salesOrderRepository.findLatestOrdersByFranchiseId(
                franchiseId, PageRequest.of(0, 5)).stream().map(this::toOrderSummaryDto).toList();

        List<InvoiceSummaryDto> latestInvoices = invoiceRepository.findLatestInvoicesByFranchiseId(
                franchiseId, PageRequest.of(0, 5)).stream().map(this::toInvoiceSummaryDto).toList();

        return FranchiseDashboardSummaryResponseDto.builder()
                .franchiseId(franchise.getId())
                .franchiseName(franchise.getBusinessName())
                .activeOutletCount(activeOutlets)
                .machineCount(machineCount)
                .ordersByStatus(ordersByStatus)
                .invoiceTotalThisMonth(invoiceTotalMonth)
                .pendingAmount(pendingAmount)
                .overdueAmount(overdueAmount)
                .overdueInvoiceCount(overdueCount)
                .nextDueInvoice(nextDueInvoice)
                .latestFiveOrders(latestOrders)
                .latestFiveInvoices(latestInvoices)
                .build();
    }

    public DueInvoiceResponseDto toDueDto(Invoice inv, LocalDate today) {
        LocalDate dueDate = inv.getDueDate();
        DueType dueType;
        DueSeverity severity;
        long daysOverdue = 0;

        if (dueDate.isBefore(today)) {
            dueType = DueType.OVERDUE;
            severity = DueSeverity.RED;
            daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
        } else if (dueDate.isEqual(today)) {
            dueType = DueType.DUE_TODAY;
            severity = DueSeverity.AMBER;
            daysOverdue = 0;
        } else {
            dueType = DueType.UPCOMING;
            severity = DueSeverity.NORMAL;
            daysOverdue = 0;
        }

        return DueInvoiceResponseDto.builder()
                .invoiceId(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .franchiseId(inv.getSalesOrder().getFranchise().getId())
                .franchiseName(inv.getSalesOrder().getFranchise().getBusinessName())
                .invoiceDate(inv.getInvoiceDate())
                .dueDate(dueDate)
                .totalAmount(inv.getTotalAmount())
                .paidAmount(inv.getPaidAmount())
                .balanceAmount(inv.getBalanceAmount())
                .paymentStatus(inv.getPaymentStatus())
                .dueType(dueType)
                .daysOverdue(daysOverdue)
                .displaySeverity(severity)
                .build();
    }

    private OrderSummaryDto toOrderSummaryDto(SalesOrder order) {
        BigDecimal totalAmount = order.getItems().stream()
                .map(item -> item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalQuantity = order.getItems().stream()
                .map(item -> item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderSummaryDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .franchiseId(order.getFranchise().getId())
                .franchiseCode(order.getFranchise().getFranchiseCode())
                .franchiseBusinessName(order.getFranchise().getBusinessName())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .itemCount(order.getItems().size())
                .totalQuantity(totalQuantity)
                .orderTotal(totalAmount)
                .createdBy(order.getCreatedBy())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private InvoiceSummaryDto toInvoiceSummaryDto(Invoice invoice) {
        return InvoiceSummaryDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .franchiseId(invoice.getSalesOrder().getFranchise().getId())
                .franchiseCode(invoice.getSalesOrder().getFranchise().getFranchiseCode())
                .franchiseBusinessName(invoice.getSalesOrder().getFranchise().getBusinessName())
                .salesOrderId(invoice.getSalesOrder().getId())
                .salesOrderNumber(invoice.getSalesOrder().getOrderNumber())
                .invoiceType(invoice.getInvoiceType())
                .paymentTerm(invoice.getPaymentTerm())
                .dueDate(invoice.getDueDate())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .balanceAmount(invoice.getBalanceAmount())
                .paymentStatus(invoice.getPaymentStatus())
                .pdfAvailable(invoice.getPdfFilePath() != null)
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
