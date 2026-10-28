package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceDueDateUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoicePaymentSummaryResponseDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceResponseDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.PaymentCollectionRequestDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.PaymentCollectionResponseDto;
import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.PaymentTerm;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.InvoiceService;
import com.asalkartechworks.daddysodahouse.service.PaymentCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Invoices, GST/Normal Billing, Due Dates, PDF Downloads, and Payment Collections (BE-06).
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices & Payments", description = "Franchise billing, GST/Normal invoices, credit due dates, PDF downloads, and manual payment collections")
@SecurityRequirement(name = "Bearer Authentication")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PaymentCollectionService paymentCollectionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Create invoice for delivered order",
            description = """
                    Generate a GST or NORMAL invoice for a DELIVERED sales order.
                    - One sales order can have only one invoice.
                    - Order items and prices are snapshotted into invoice_items.
                    - For NORMAL: GST is 0%; total = subtotal.
                    - For GST: GST percentage is configurable (default: 18%); total = subtotal + GST.
                    - For CREDIT / PARTIAL_PAYMENT: due date defaults to invoiceDate + 10 days if omitted.
                    - Server generates a downloadable PDF file automatically.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Invoice creation payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = InvoiceCreateRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "GST Invoice Example",
                                            description = "GST invoice with 18% tax and 10-day credit term",
                                            value = """
                                                    {
                                                      "salesOrderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "invoiceType": "GST",
                                                      "invoiceDate": "2026-09-23",
                                                      "paymentTerm": "CREDIT",
                                                      "dueDate": "2026-10-03",
                                                      "gstRatePercent": 18.00,
                                                      "notes": "Standard 10-day credit term"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Normal Invoice Example",
                                            description = "Normal invoice without GST calculation",
                                            value = """
                                                    {
                                                      "salesOrderId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "invoiceType": "NORMAL",
                                                      "invoiceDate": "2026-09-23",
                                                      "paymentTerm": "FULL_PAYMENT",
                                                      "notes": "Full payment received on delivery"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> createInvoice(
            @Valid @RequestBody InvoiceCreateRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceResponseDto response = invoiceService.createInvoice(dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Invoice created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "List invoices",
            description = "Get paginated invoice summaries with dynamic filters. Franchise owners are strictly isolated to their own franchise."
    )
    public ResponseEntity<ApiResponse<PageResponse<InvoiceSummaryDto>>> getAllInvoices(
            @Parameter(description = "Filter by franchise ID (Managers/Admins only)") @RequestParam(required = false) UUID franchiseId,
            @Parameter(description = "Filter by sales order ID") @RequestParam(required = false) UUID salesOrderId,
            @Parameter(description = "Filter by invoice number") @RequestParam(required = false) String invoiceNumber,
            @Parameter(description = "Filter by invoice type (GST, NORMAL)") @RequestParam(required = false) InvoiceType invoiceType,
            @Parameter(description = "Filter by payment status (UNPAID, PARTIALLY_PAID, PAID, OVERDUE)") @RequestParam(required = false) InvoicePaymentStatus paymentStatus,
            @Parameter(description = "Filter by payment term (FULL_PAYMENT, PARTIAL_PAYMENT, CREDIT)") @RequestParam(required = false) PaymentTerm paymentTerm,
            @Parameter(description = "Filter by invoice date from (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Filter by invoice date to (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Filter by due date from (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @Parameter(description = "Filter by due date to (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @Parameter(description = "Filter overdue invoices only") @RequestParam(required = false) Boolean overdueOnly,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        PageResponse<InvoiceSummaryDto> response = invoiceService.getAllInvoices(
                franchiseId, salesOrderId, invoiceNumber, invoiceType, paymentStatus,
                paymentTerm, fromDate, toDate, dueFrom, dueTo, overdueOnly, pageable, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Invoices retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Get invoice by ID",
            description = "Retrieve full invoice details with items, tax breakdown, payment ledger, and PDF download link. Franchise owners can only view their own franchise invoices."
    )
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> getInvoiceById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceResponseDto response = invoiceService.getInvoiceById(id, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice details retrieved successfully"));
    }

    @PatchMapping("/{id}/due-date")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Update invoice due date",
            description = "Update due date for an unpaid or partially paid credit invoice. Reason is mandatory (max 500 characters). Regenerates invoice PDF."
    )
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> updateDueDate(
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceDueDateUpdateRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceResponseDto response = invoiceService.updateDueDate(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Invoice due date updated successfully"));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Record manual payment collection",
            description = """
                    Record a manual payment entry (CASH, UPI, BANK_TRANSFER, CHEQUE, OTHER) for an invoice.
                    - Amount must be positive and cannot exceed outstanding balance.
                    - Recalculates paid amount, balance amount, and payment status (PAID if balance is 0).
                    - Regenerates invoice PDF with updated payment details.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Payment collection entry",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PaymentCollectionRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "UPI Payment Example",
                                            value = """
                                                    {
                                                      "paymentDate": "2026-09-25",
                                                      "amount": 2500.00,
                                                      "paymentMode": "UPI",
                                                      "referenceNumber": "UPI-9876543210",
                                                      "remarks": "Payment received via PhonePe QR"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    public ResponseEntity<ApiResponse<InvoicePaymentSummaryResponseDto>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentCollectionRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoicePaymentSummaryResponseDto response = paymentCollectionService.recordPayment(id, dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment collection recorded successfully"));
    }

    @PostMapping("/{id}/apply-credit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Apply franchise credit to due invoice",
            description = "Apply available franchise credit points to settle or partially pay an invoice balance."
    )
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> applyCredit(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, BigDecimal> requestBody,
            @AuthenticationPrincipal UserPrincipal principal) {
        BigDecimal amount = (requestBody != null && requestBody.containsKey("amount")) ? requestBody.get("amount") : null;
        InvoiceResponseDto response = invoiceService.applyCredit(id, amount, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Franchise credit applied successfully"));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "List payment history for invoice",
            description = "Retrieve list of all manual payment collections recorded for an invoice, ordered newest first."
    )
    public ResponseEntity<ApiResponse<List<PaymentCollectionResponseDto>>> getPaymentsByInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<PaymentCollectionResponseDto> response = paymentCollectionService.getPaymentsByInvoice(id, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment history retrieved successfully"));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Download invoice PDF",
            description = "Download the generated PDF for an invoice. If missing on disk, it is dynamically regenerated from the invoice snapshot."
    )
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        byte[] pdfBytes = invoiceService.downloadInvoicePdf(id, principal);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
