package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.invoice.InvoicePaymentSummaryResponseDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.PaymentCollectionRequestDto;
import com.asalkartechworks.daddysodahouse.dto.invoice.PaymentCollectionResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Invoice;
import com.asalkartechworks.daddysodahouse.entity.PaymentCollection;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.InvoiceRepository;
import com.asalkartechworks.daddysodahouse.repository.PaymentCollectionRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCollectionService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentCollectionRepository paymentCollectionRepository;
    private final InvoicePdfService invoicePdfService;

    /**
     * Record a manual payment collection against an invoice.
     */
    @Transactional
    public InvoicePaymentSummaryResponseDto recordPayment(UUID invoiceId, PaymentCollectionRequestDto dto, UserPrincipal principal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        if (dto.getPaymentDate() == null || dto.getPaymentDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Payment date cannot be in the future: " + dto.getPaymentDate());
        }

        if (dto.getAmount().compareTo(invoice.getBalanceAmount()) > 0) {
            throw new BadRequestException(String.format(
                    "Payment amount %s exceeds current invoice balance %s",
                    dto.getAmount(), invoice.getBalanceAmount()
            ));
        }

        PaymentCollection payment = PaymentCollection.builder()
                .invoice(invoice)
                .paymentDate(dto.getPaymentDate())
                .amount(dto.getAmount())
                .paymentMode(dto.getPaymentMode())
                .referenceNumber(dto.getReferenceNumber())
                .remarks(dto.getRemarks())
                .createdBy(principal != null ? principal.getId() : null)
                .build();

        PaymentCollection savedPayment = paymentCollectionRepository.save(payment);

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(dto.getAmount());
        BigDecimal newBalanceAmount = invoice.getTotalAmount().subtract(newPaidAmount);

        invoice.setPaidAmount(newPaidAmount);
        invoice.setBalanceAmount(newBalanceAmount);
        invoice.recalculatePaymentStatus(LocalDate.now());

        invoiceRepository.save(invoice);

        // Regenerate PDF to show updated payment ledger
        try {
            invoicePdfService.generateAndSavePdf(invoice);
        } catch (Exception e) {
            log.warn("Failed to regenerate PDF after payment collection for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
        }

        log.info("Recorded payment of {} for invoice {}. New balance: {}, Status: {}",
                dto.getAmount(), invoice.getInvoiceNumber(), newBalanceAmount, invoice.getPaymentStatus());

        PaymentCollectionResponseDto paymentDto = PaymentCollectionResponseDto.builder()
                .id(savedPayment.getId())
                .invoiceId(invoice.getId())
                .paymentDate(savedPayment.getPaymentDate())
                .amount(savedPayment.getAmount())
                .paymentMode(savedPayment.getPaymentMode())
                .referenceNumber(savedPayment.getReferenceNumber())
                .remarks(savedPayment.getRemarks())
                .createdAt(savedPayment.getCreatedAt())
                .createdBy(savedPayment.getCreatedBy())
                .build();

        return InvoicePaymentSummaryResponseDto.builder()
                .payment(paymentDto)
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .balanceAmount(invoice.getBalanceAmount())
                .paymentStatus(invoice.getPaymentStatus())
                .build();
    }

    /**
     * Retrieve payment history for an invoice with role-based isolation.
     */
    @Transactional(readOnly = true)
    public List<PaymentCollectionResponseDto> getPaymentsByInvoice(UUID invoiceId, UserPrincipal principal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        verifyFranchiseOwnershipIfOwner(invoice, principal);

        List<PaymentCollection> payments = paymentCollectionRepository.findByInvoiceIdOrderByPaymentDateDescCreatedAtDesc(invoiceId);

        return payments.stream().map(p -> PaymentCollectionResponseDto.builder()
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
}
