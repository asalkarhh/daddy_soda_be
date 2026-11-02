package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.entity.Invoice;
import com.asalkartechworks.daddysodahouse.entity.InvoiceItem;
import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.PurchaseEntry;
import com.asalkartechworks.daddysodahouse.entity.PurchaseItem;
import com.asalkartechworks.daddysodahouse.entity.SalesOrder;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderItem;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import com.asalkartechworks.daddysodahouse.repository.InvoiceRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseEntryRepository;
import com.asalkartechworks.daddysodahouse.repository.SalesOrderRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.util.CsvExportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for streaming monthly CSV export reports.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final PurchaseEntryRepository purchaseEntryRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Generate monthly purchases CSV report.
     */
    @Transactional(readOnly = true)
    public byte[] generatePurchasesReportCsv(String month, UUID supplierId) {
        YearMonth yearMonth = parseAndValidateMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<PurchaseEntry> purchases = purchaseEntryRepository.findPurchasesForReport(supplierId, start, end);

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        sb.append("\uFEFF");

        // Header
        sb.append(CsvExportUtil.formatRow(Arrays.asList(
                "Purchase Number",
                "Bill Date",
                "Supplier",
                "Supplier Bill Number",
                "Product Code",
                "Flavour Name",
                "Bottle Size Litre",
                "Quantity",
                "Purchase Status",
                "Description",
                "Created By",
                "Created At"
        )));

        // Rows
        for (PurchaseEntry pe : purchases) {
            String purchaseNumber = pe.getPurchaseNumber() != null ? pe.getPurchaseNumber() : "";
            String billDateStr = pe.getBillDate() != null ? pe.getBillDate().toString() : "";
            String supplierName = pe.getSupplier() != null ? pe.getSupplier().getSupplierName() : "";
            String supplierBillNo = pe.getSupplierBillNumber() != null ? pe.getSupplierBillNumber() : "";
            String statusStr = pe.getStatus() != null ? pe.getStatus().toString() : "";
            String descStr = pe.getDescription() != null ? pe.getDescription() : "";
            String createdByStr = pe.getCreatedBy() != null ? pe.getCreatedBy().toString() : "";
            String createdAtStr = pe.getCreatedAt() != null ? pe.getCreatedAt().toString() : "";

            if (pe.getItems() == null || pe.getItems().isEmpty()) {
                sb.append(CsvExportUtil.formatRow(Arrays.asList(
                        purchaseNumber,
                        billDateStr,
                        supplierName,
                        supplierBillNo,
                        "",
                        "",
                        "",
                        "",
                        statusStr,
                        descStr,
                        createdByStr,
                        createdAtStr
                )));
            } else {
                for (PurchaseItem item : pe.getItems()) {
                    String itemCode = (item.getProduct() != null && item.getProduct().getItemCode() != null)
                            ? item.getProduct().getItemCode() : "";
                    String flavourName = (item.getProduct() != null && item.getProduct().getFlavourName() != null)
                            ? item.getProduct().getFlavourName() : "";
                    String bottleSize = (item.getProduct() != null && item.getProduct().getBottleSizeLitre() != null)
                            ? item.getProduct().getBottleSizeLitre().toString() : "";
                    String qtyStr = item.getQuantity() != null ? item.getQuantity().toString() : "";

                    sb.append(CsvExportUtil.formatRow(Arrays.asList(
                            purchaseNumber,
                            billDateStr,
                            supplierName,
                            supplierBillNo,
                            itemCode,
                            flavourName,
                            bottleSize,
                            qtyStr,
                            statusStr,
                            descStr,
                            createdByStr,
                            createdAtStr
                    )));
                }
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate monthly sales orders CSV report.
     */
    @Transactional(readOnly = true)
    public byte[] generateOrdersReportCsv(String month, UUID requestedFranchiseId, SalesOrderStatus status, UserPrincipal principal) {
        YearMonth yearMonth = parseAndValidateMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        UUID effectiveFranchiseId = resolveEffectiveFranchiseId(requestedFranchiseId, principal);

        List<SalesOrder> orders = salesOrderRepository.findMonthlyOrdersForReport(effectiveFranchiseId, status, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");

        // Header
        sb.append(CsvExportUtil.formatRow(Arrays.asList(
                "Order Number",
                "Order Date",
                "Franchise Name",
                "Status",
                "Product Code",
                "Flavour Name",
                "Quantity",
                "Unit Price",
                "Line Total",
                "Notes",
                "Created By",
                "Delivered At"
        )));

        // Rows
        for (SalesOrder o : orders) {
            String orderNumber = o.getOrderNumber() != null ? o.getOrderNumber() : "";
            String orderDateStr = o.getOrderDate() != null ? o.getOrderDate().toString() : "";
            String franchiseName = o.getFranchise() != null ? o.getFranchise().getBusinessName() : "";
            String statusStr = o.getStatus() != null ? o.getStatus().toString() : "";
            String notes = o.getNotes() != null ? o.getNotes() : "";
            String createdByStr = o.getCreatedBy() != null ? o.getCreatedBy().toString() : "";
            String deliveredAtStr = o.getDeliveredAt() != null ? o.getDeliveredAt().toString() : "";

            if (o.getItems() == null || o.getItems().isEmpty()) {
                sb.append(CsvExportUtil.formatRow(Arrays.asList(
                        orderNumber,
                        orderDateStr,
                        franchiseName,
                        statusStr,
                        "",
                        "",
                        "",
                        "",
                        "",
                        notes,
                        createdByStr,
                        deliveredAtStr
                )));
            } else {
                for (SalesOrderItem item : o.getItems()) {
                    String itemCode = (item.getProduct() != null && item.getProduct().getItemCode() != null)
                            ? item.getProduct().getItemCode() : "";
                    String flavourName = (item.getProduct() != null && item.getProduct().getFlavourName() != null)
                            ? item.getProduct().getFlavourName() : "";
                    String qtyStr = item.getQuantity() != null ? item.getQuantity().toString() : "";
                    String unitPriceStr = item.getUnitPrice() != null ? item.getUnitPrice().toString() : "";
                    String lineTotalStr = item.getLineTotal() != null ? item.getLineTotal().toString() : "";

                    sb.append(CsvExportUtil.formatRow(Arrays.asList(
                            orderNumber,
                            orderDateStr,
                            franchiseName,
                            statusStr,
                            itemCode,
                            flavourName,
                            qtyStr,
                            unitPriceStr,
                            lineTotalStr,
                            notes,
                            createdByStr,
                            deliveredAtStr
                    )));
                }
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate monthly invoices CSV report.
     */
    @Transactional(readOnly = true)
    public byte[] generateInvoicesReportCsv(
            String month,
            UUID requestedFranchiseId,
            InvoiceType invoiceType,
            InvoicePaymentStatus paymentStatus,
            UserPrincipal principal) {

        YearMonth yearMonth = parseAndValidateMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        UUID effectiveFranchiseId = resolveEffectiveFranchiseId(requestedFranchiseId, principal);

        List<Invoice> invoices = invoiceRepository.findMonthlyInvoicesForReport(
                effectiveFranchiseId, invoiceType, paymentStatus, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");

        // Header
        sb.append(CsvExportUtil.formatRow(Arrays.asList(
                "Invoice Number",
                "Invoice Date",
                "Franchise Name",
                "Invoice Type",
                "Payment Term",
                "Due Date",
                "Subtotal",
                "GST Rate",
                "GST Amount",
                "Total Amount",
                "Paid Amount",
                "Balance Amount",
                "Payment Status",
                "Product Code",
                "Flavour Name",
                "Quantity",
                "Unit Price",
                "Line Total"
        )));

        // Rows
        for (Invoice inv : invoices) {
            String invNumber = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "";
            String invDateStr = inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : "";
            String franchiseName = inv.getSalesOrder() != null && inv.getSalesOrder().getFranchise() != null
                    ? inv.getSalesOrder().getFranchise().getBusinessName() : "";
            String invTypeStr = inv.getInvoiceType() != null ? inv.getInvoiceType().toString() : "";
            String paymentTermStr = inv.getPaymentTerm() != null ? inv.getPaymentTerm().toString() : "";
            String dueDateStr = inv.getDueDate() != null ? inv.getDueDate().toString() : "";
            String subtotalStr = inv.getSubtotalAmount() != null ? inv.getSubtotalAmount().toString() : "";
            String gstRateStr = inv.getGstRatePercent() != null ? inv.getGstRatePercent().toString() : "";
            String gstAmountStr = inv.getGstAmount() != null ? inv.getGstAmount().toString() : "";
            String totalAmountStr = inv.getTotalAmount() != null ? inv.getTotalAmount().toString() : "";
            String paidAmountStr = inv.getPaidAmount() != null ? inv.getPaidAmount().toString() : "";
            String balanceAmountStr = inv.getBalanceAmount() != null ? inv.getBalanceAmount().toString() : "";
            String paymentStatusStr = inv.getPaymentStatus() != null ? inv.getPaymentStatus().toString() : "";

            if (inv.getItems() == null || inv.getItems().isEmpty()) {
                sb.append(CsvExportUtil.formatRow(Arrays.asList(
                        invNumber,
                        invDateStr,
                        franchiseName,
                        invTypeStr,
                        paymentTermStr,
                        dueDateStr,
                        subtotalStr,
                        gstRateStr,
                        gstAmountStr,
                        totalAmountStr,
                        paidAmountStr,
                        balanceAmountStr,
                        paymentStatusStr,
                        "",
                        "",
                        "",
                        "",
                        ""
                )));
            } else {
                for (InvoiceItem item : inv.getItems()) {
                    String itemCode = item.getItemCode() != null ? item.getItemCode() : "";
                    String flavourName = item.getFlavourName() != null ? item.getFlavourName() : "";
                    String qtyStr = item.getQuantity() != null ? item.getQuantity().toString() : "";
                    String unitPriceStr = item.getUnitPrice() != null ? item.getUnitPrice().toString() : "";
                    String lineTotalStr = item.getLineTotal() != null ? item.getLineTotal().toString() : "";

                    sb.append(CsvExportUtil.formatRow(Arrays.asList(
                            invNumber,
                            invDateStr,
                            franchiseName,
                            invTypeStr,
                            paymentTermStr,
                            dueDateStr,
                            subtotalStr,
                            gstRateStr,
                            gstAmountStr,
                            totalAmountStr,
                            paidAmountStr,
                            balanceAmountStr,
                            paymentStatusStr,
                            itemCode,
                            flavourName,
                            qtyStr,
                            unitPriceStr,
                            lineTotalStr
                    )));
                }
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate monthly dues CSV report.
     */
    @Transactional(readOnly = true)
    public byte[] generateDuesReportCsv(String month, UUID requestedFranchiseId, UserPrincipal principal) {
        YearMonth yearMonth = parseAndValidateMonth(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();

        UUID effectiveFranchiseId = resolveEffectiveFranchiseId(requestedFranchiseId, principal);

        List<Invoice> dues = invoiceRepository.findMonthlyDuesForReport(effectiveFranchiseId, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");

        // Header
        sb.append(CsvExportUtil.formatRow(Arrays.asList(
                "Invoice Number",
                "Franchise Name",
                "Invoice Date",
                "Due Date",
                "Total Amount",
                "Paid Amount",
                "Balance Amount",
                "Payment Status",
                "Due Type",
                "Days Overdue"
        )));

        // Rows
        for (Invoice inv : dues) {
            String invNumber = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "";
            String franchiseName = inv.getSalesOrder() != null && inv.getSalesOrder().getFranchise() != null
                    ? inv.getSalesOrder().getFranchise().getBusinessName() : "";
            String invDateStr = inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : "";
            LocalDate dueDate = inv.getDueDate();
            String dueDateStr = dueDate != null ? dueDate.toString() : "";
            String totalAmountStr = inv.getTotalAmount() != null ? inv.getTotalAmount().toString() : "";
            String paidAmountStr = inv.getPaidAmount() != null ? inv.getPaidAmount().toString() : "";
            String balanceAmountStr = inv.getBalanceAmount() != null ? inv.getBalanceAmount().toString() : "";
            String paymentStatusStr = inv.getPaymentStatus() != null ? inv.getPaymentStatus().toString() : "";

            String dueType;
            long daysOverdue = 0;
            if (dueDate != null) {
                if (dueDate.isBefore(today)) {
                    dueType = "OVERDUE";
                    daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
                } else if (dueDate.isEqual(today)) {
                    dueType = "DUE_TODAY";
                } else {
                    dueType = "UPCOMING";
                }
            } else {
                dueType = "N/A";
            }

            sb.append(CsvExportUtil.formatRow(Arrays.asList(
                    invNumber,
                    franchiseName,
                    invDateStr,
                    dueDateStr,
                    totalAmountStr,
                    paidAmountStr,
                    balanceAmountStr,
                    paymentStatusStr,
                    dueType,
                    daysOverdue
            )));
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private YearMonth parseAndValidateMonth(String month) {
        if (month == null || !MONTH_PATTERN.matcher(month.trim()).matches()) {
            throw new BadRequestException("Invalid month format: '" + month + "'. Expected YYYY-MM (e.g. 2026-09)");
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid month date format: " + month);
        }
    }

    private UUID resolveEffectiveFranchiseId(UUID requestedFranchiseId, UserPrincipal principal) {
        if (principal != null && principal.getRole() == UserRole.FRANCHISE_OWNER) {
            UUID ownerFranchiseId = principal.getFranchiseId();
            if (ownerFranchiseId == null) {
                throw new AccessDeniedException("Franchise owner is not associated with any franchise");
            }
            return ownerFranchiseId;
        }
        return requestedFranchiseId;
    }
}
