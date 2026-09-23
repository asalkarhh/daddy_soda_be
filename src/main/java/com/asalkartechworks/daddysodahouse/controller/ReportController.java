package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller for downloading monthly operational, sales, invoicing, and credit dues CSV reports.
 */
@RestController
@RequestMapping("/api/v1/reports/monthly")
@RequiredArgsConstructor
@Tag(name = "Monthly Reports", description = "Downloadable monthly CSV reports for purchases, orders, invoices, and credit dues")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/purchases/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Download monthly purchases report (CSV)", description = "Streams CSV export of monthly purchase bills with line-item breakdown")
    public ResponseEntity<byte[]> downloadPurchasesReport(
            @RequestParam String month,
            @RequestParam(required = false) UUID supplierId) {

        byte[] csvData = reportService.generatePurchasesReportCsv(month, supplierId);
        String filename = String.format("monthly-purchases-%s.csv", month.trim());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvData);
    }

    @GetMapping("/orders/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(summary = "Download monthly sales orders report (CSV)", description = "Streams CSV export of monthly sales orders. Scoped to authenticated franchise for Franchise Owners.")
    public ResponseEntity<byte[]> downloadOrdersReport(
            @RequestParam String month,
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) SalesOrderStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] csvData = reportService.generateOrdersReportCsv(month, franchiseId, status, principal);
        String filename = String.format("monthly-orders-%s.csv", month.trim());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvData);
    }

    @GetMapping("/invoices/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(summary = "Download monthly invoices report (CSV)", description = "Streams CSV export of monthly invoices, taxes, and line items. Scoped to authenticated franchise for Franchise Owners.")
    public ResponseEntity<byte[]> downloadInvoicesReport(
            @RequestParam String month,
            @RequestParam(required = false) UUID franchiseId,
            @RequestParam(required = false) InvoiceType invoiceType,
            @RequestParam(required = false) InvoicePaymentStatus paymentStatus,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] csvData = reportService.generateInvoicesReportCsv(month, franchiseId, invoiceType, paymentStatus, principal);
        String filename = String.format("monthly-invoices-%s.csv", month.trim());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvData);
    }

    @GetMapping("/dues/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(summary = "Download monthly credit dues report (CSV)", description = "Streams CSV export of outstanding credit dues for the month. Scoped to authenticated franchise for Franchise Owners.")
    public ResponseEntity<byte[]> downloadDuesReport(
            @RequestParam String month,
            @RequestParam(required = false) UUID franchiseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        byte[] csvData = reportService.generateDuesReportCsv(month, franchiseId, principal);
        String filename = String.format("monthly-dues-%s.csv", month.trim());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvData);
    }
}
