package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.config.InvoiceProperties;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceDueDateUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceItemResponseDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceResponseDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.PaymentCollectionResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Invoice;
import com.asalkartechworks.daddysodahouse.entity.InvoiceItem;
import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.PaymentTerm;
import com.asalkartechworks.daddysodahouse.entity.SalesOrder;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderItem;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.InvoiceRepository;
import com.asalkartechworks.daddysodahouse.repository.PaymentCollectionRepository;
import com.asalkartechworks.daddysodahouse.repository.SalesOrderRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PaymentCollectionRepository paymentCollectionRepository;
    private final InvoicePdfService invoicePdfService;
    private final InvoiceProperties invoiceProperties;

    /**
     * Create an invoice for a PREPARED or DELIVERED sales order.
     */
    @Transactional
    public InvoiceResponseDto createInvoice(InvoiceCreateRequestDto dto, UserPrincipal principal) {
        SalesOrder salesOrder = salesOrderRepository.findById(dto.getSalesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", dto.getSalesOrderId()));

        if (salesOrder.getStatus() != SalesOrderStatus.PREPARED && salesOrder.getStatus() != SalesOrderStatus.DELIVERED) {
            throw new BadRequestException("Invoices can only be created for PREPARED or DELIVERED sales orders. Current status: " + salesOrder.getStatus());
        }

        if (invoiceRepository.existsBySalesOrderId(salesOrder.getId())) {
            throw new BadRequestException("An invoice already exists for sales order: " + salesOrder.getOrderNumber());
        }

        LocalDate invoiceDate = dto.getInvoiceDate() != null ? dto.getInvoiceDate() : LocalDate.now();
        if (invoiceDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Invoice date cannot be in the future: " + invoiceDate);
        }

        // Subtotal calculation from delivered order items
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SalesOrderItem orderItem : salesOrder.getItems()) {
            if (orderItem.getLineTotal() != null) {
                subtotal = subtotal.add(orderItem.getLineTotal());
            }
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        // GST & Total calculations
        BigDecimal gstRatePercent;
        BigDecimal gstAmount;
        BigDecimal totalAmount;

        if (dto.getInvoiceType() == InvoiceType.NORMAL) {
            gstRatePercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            gstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            totalAmount = subtotal;
        } else {
            gstRatePercent = dto.getGstRatePercent() != null
                    ? dto.getGstRatePercent().setScale(2, RoundingMode.HALF_UP)
                    : invoiceProperties.getDefaultGstRatePercent().setScale(2, RoundingMode.HALF_UP);
            gstAmount = subtotal.multiply(gstRatePercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            totalAmount = subtotal.add(gstAmount).setScale(2, RoundingMode.HALF_UP);
        }

        // Due date calculation
        LocalDate dueDate = null;
        if (dto.getPaymentTerm() == PaymentTerm.FULL_PAYMENT) {
            dueDate = null;
        } else {
            if (dto.getDueDate() != null) {
                if (dto.getDueDate().isBefore(invoiceDate)) {
                    throw new BadRequestException("Due date cannot be before invoice date: " + dto.getDueDate());
                }
                dueDate = dto.getDueDate();
            } else {
                dueDate = invoiceDate.plusDays(10);
            }
        }

        String invoiceNumber = generateInvoiceNumber(dto.getInvoiceType());

        Invoice invoice = Invoice.builder()
                .salesOrder(salesOrder)
                .invoiceNumber(invoiceNumber)
                .invoiceType(dto.getInvoiceType())
                .invoiceDate(invoiceDate)
                .paymentTerm(dto.getPaymentTerm())
                .dueDate(dueDate)
                .subtotalAmount(subtotal)
                .gstRatePercent(gstRatePercent)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .paidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .balanceAmount(totalAmount)
                .paymentStatus(InvoicePaymentStatus.UNPAID)
                .notes(StringUtils.hasText(dto.getNotes()) ? dto.getNotes().trim() : null)
                .build();

        // Copy item snapshots from sales order
        for (SalesOrderItem orderItem : salesOrder.getItems()) {
            InvoiceItem invoiceItem = InvoiceItem.builder()
                    .invoice(invoice)
                    .product(orderItem.getProduct())
                    .itemCode(orderItem.getProduct().getItemCode())
                    .flavourName(orderItem.getProduct().getFlavourName())
                    .bottleSizeLitre(orderItem.getProduct().getBottleSizeLitre())
                    .quantity(orderItem.getQuantity())
                    .unitPrice(orderItem.getUnitPrice())
                    .lineTotal(orderItem.getLineTotal())
                    .build();
            invoice.addItem(invoiceItem);
        }

        Invoice saved = invoiceRepository.save(invoice);

        // Generate and save PDF file
        try {
            String pdfPath = invoicePdfService.generateAndSavePdf(saved);
            saved.setPdfFilePath(pdfPath);
            saved.setPdfGeneratedAt(Instant.now());
            saved = invoiceRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to generate initial PDF for invoice {}: {}", saved.getInvoiceNumber(), e.getMessage());
        }

        log.info("Created invoice {} for sales order {} with total {}",
                saved.getInvoiceNumber(), salesOrder.getOrderNumber(), saved.getTotalAmount());

        return toResponseDto(saved);
    }

    /**
     * Update due date for an unpaid or partially paid credit invoice.
     */
    @Transactional
    public InvoiceResponseDto updateDueDate(UUID id, InvoiceDueDateUpdateRequestDto dto, UserPrincipal principal) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Cannot update due date for a fully paid invoice");
        }

        if (invoice.getPaymentTerm() == PaymentTerm.FULL_PAYMENT) {
            throw new BadRequestException("Due date updates are not applicable for FULL_PAYMENT invoices");
        }

        if (dto.getDueDate().isBefore(invoice.getInvoiceDate())) {
            throw new BadRequestException("Due date cannot be before invoice date: " + dto.getDueDate());
        }

        invoice.setDueDate(dto.getDueDate());
        invoice.recalculatePaymentStatus(LocalDate.now());

        String updateAudit = String.format("[Due date updated to %s. Reason: %s]", dto.getDueDate(), dto.getReason().trim());
        if (StringUtils.hasText(invoice.getNotes())) {
            invoice.setNotes(invoice.getNotes() + " " + updateAudit);
        } else {
            invoice.setNotes(updateAudit);
        }

        // Regenerate PDF with updated due date
        try {
            String pdfPath = invoicePdfService.generateAndSavePdf(invoice);
            invoice.setPdfFilePath(pdfPath);
            invoice.setPdfGeneratedAt(Instant.now());
        } catch (Exception e) {
            log.warn("Failed to regenerate PDF after due date update for {}: {}", invoice.getInvoiceNumber(), e.getMessage());
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Updated due date for invoice {} to {}. Status: {}", saved.getInvoiceNumber(), saved.getDueDate(), saved.getPaymentStatus());
        return toResponseDto(saved);
    }

    /**
     * Get single invoice by ID with franchise isolation for FRANCHISE_OWNER.
     */
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceById(UUID id, UserPrincipal principal) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        verifyFranchiseOwnershipIfOwner(invoice, principal);
        return toResponseDto(invoice);
    }

    /**
     * List invoices with dynamic filters.
     */
    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryDto> getAllInvoices(
            UUID franchiseId,
            UUID salesOrderId,
            String invoiceNumber,
            InvoiceType invoiceType,
            InvoicePaymentStatus paymentStatus,
            PaymentTerm paymentTerm,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDate dueFrom,
            LocalDate dueTo,
            Boolean overdueOnly,
            Pageable pageable,
            UserPrincipal principal) {

        UUID effectiveFranchiseId = franchiseId;
        if (principal != null && principal.getRole() == UserRole.FRANCHISE_OWNER) {
            effectiveFranchiseId = principal.getFranchiseId();
            if (effectiveFranchiseId == null) {
                throw new BadRequestException("No franchise mapped to the authenticated franchise owner");
            }
        }

        Page<Invoice> page = invoiceRepository.searchInvoices(
                effectiveFranchiseId, salesOrderId, invoiceNumber, invoiceType,
                paymentStatus, paymentTerm, fromDate, toDate, dueFrom, dueTo,
                overdueOnly, pageable);

        return PageResponse.from(page.map(this::toSummaryDto));
    }

    /**
     * List invoices strictly for the logged-in franchise owner.
     */
    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryDto> getMyInvoices(
            InvoicePaymentStatus paymentStatus,
            InvoiceType invoiceType,
            PaymentTerm paymentTerm,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDate dueFrom,
            LocalDate dueTo,
            Boolean overdueOnly,
            Pageable pageable,
            UserPrincipal principal) {

        UUID franchiseId = principal.getFranchiseId();
        if (franchiseId == null) {
            throw new ResourceNotFoundException("Franchise mapping", "user", principal.getId());
        }

        Page<Invoice> page = invoiceRepository.searchInvoices(
                franchiseId, null, null, invoiceType, paymentStatus,
                paymentTerm, fromDate, toDate, dueFrom, dueTo, overdueOnly, pageable);

        return PageResponse.from(page.map(this::toSummaryDto));
    }

    /**
     * Retrieve PDF binary bytes for download; auto-regenerate if missing on disk.
     */
    @Transactional(readOnly = true)
    public byte[] downloadInvoicePdf(UUID id, UserPrincipal principal) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        verifyFranchiseOwnershipIfOwner(invoice, principal);

        return invoicePdfService.getOrRegeneratePdfBytes(invoice);
    }

    /**
     * Refresh overdue statuses for invoices whose due date has passed.
     */
    @Transactional
    public int refreshOverdueInvoices() {
        List<Invoice> overdueInvoices = invoiceRepository.findInvoicesBecomingOverdue(LocalDate.now());
        for (Invoice invoice : overdueInvoices) {
            invoice.setPaymentStatus(InvoicePaymentStatus.OVERDUE);
        }
        invoiceRepository.saveAll(overdueInvoices);
        log.info("Refreshed overdue status for {} invoices", overdueInvoices.size());
        return overdueInvoices.size();
    }

    private synchronized String generateInvoiceNumber(InvoiceType type) {
        LocalDate now = LocalDate.now();
        String prefix = String.format("%s-%d%02d-", type == InvoiceType.GST ? "GST" : "INV", now.getYear(), now.getMonthValue());

        return invoiceRepository.findMaxInvoiceNumberWithPrefix(prefix)
                .map(maxNumber -> {
                    try {
                        String seqStr = maxNumber.substring(prefix.length());
                        long seq = Long.parseLong(seqStr) + 1;
                        return String.format("%s%05d", prefix, seq);
                    } catch (Exception e) {
                        return String.format("%s%05d", prefix, 1);
                    }
                })
                .orElse(String.format("%s%05d", prefix, 1));
    }

    private void verifyFranchiseOwnershipIfOwner(Invoice invoice, UserPrincipal principal) {
        if (principal != null && principal.getRole() == UserRole.FRANCHISE_OWNER) {
            UUID userFranchiseId = principal.getFranchiseId();
            UUID invoiceFranchiseId = invoice.getSalesOrder().getFranchise().getId();
            if (userFranchiseId == null || !userFranchiseId.equals(invoiceFranchiseId)) {
                throw new AccessDeniedException("Access denied: You do not have permission to access invoices of another franchise");
            }
        }
    }

    private InvoiceResponseDto toResponseDto(Invoice invoice) {
        List<InvoiceItemResponseDto> itemDtos = invoice.getItems().stream().map(i ->
                InvoiceItemResponseDto.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .itemCode(i.getItemCode())
                        .flavourName(i.getFlavourName())
                        .bottleSizeLitre(i.getBottleSizeLitre())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build()
        ).toList();

        List<PaymentCollectionResponseDto> paymentDtos = invoice.getPayments().stream().map(p ->
                PaymentCollectionResponseDto.builder()
                        .id(p.getId())
                        .invoiceId(invoice.getId())
                        .paymentDate(p.getPaymentDate())
                        .amount(p.getAmount())
                        .paymentMode(p.getPaymentMode())
                        .referenceNumber(p.getReferenceNumber())
                        .remarks(p.getRemarks())
                        .createdAt(p.getCreatedAt())
                        .createdBy(p.getCreatedBy())
                        .build()
        ).toList();

        String downloadUrl = "/api/v1/invoices/" + invoice.getId() + "/download";

        return InvoiceResponseDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceType(invoice.getInvoiceType())
                .invoiceDate(invoice.getInvoiceDate())
                .paymentTerm(invoice.getPaymentTerm())
                .dueDate(invoice.getDueDate())
                .subtotalAmount(invoice.getSubtotalAmount())
                .gstRatePercent(invoice.getGstRatePercent())
                .gstAmount(invoice.getGstAmount())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .balanceAmount(invoice.getBalanceAmount())
                .paymentStatus(invoice.getPaymentStatus())
                .pdfDownloadUrl(downloadUrl)
                .pdfGeneratedAt(invoice.getPdfGeneratedAt())
                .notes(invoice.getNotes())
                .salesOrderId(invoice.getSalesOrder().getId())
                .salesOrderNumber(invoice.getSalesOrder().getOrderNumber())
                .franchiseId(invoice.getSalesOrder().getFranchise().getId())
                .franchiseCode(invoice.getSalesOrder().getFranchise().getFranchiseCode())
                .franchiseBusinessName(invoice.getSalesOrder().getFranchise().getBusinessName())
                .franchiseOwnerName(invoice.getSalesOrder().getFranchise().getOwnerName())
                .franchiseMobile(invoice.getSalesOrder().getFranchise().getMobile())
                .items(itemDtos)
                .payments(paymentDtos)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .createdBy(invoice.getCreatedBy())
                .updatedBy(invoice.getUpdatedBy())
                .build();
    }

    private InvoiceSummaryDto toSummaryDto(Invoice invoice) {
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
