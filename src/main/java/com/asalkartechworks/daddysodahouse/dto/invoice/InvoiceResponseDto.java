package com.asalkartechworks.daddysodahouse.dto.invoice;

import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.PaymentTerm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDto {

    private UUID id;
    private String invoiceNumber;
    private InvoiceType invoiceType;
    private LocalDate invoiceDate;
    private PaymentTerm paymentTerm;
    private LocalDate dueDate;
    private BigDecimal subtotalAmount;
    private BigDecimal gstRatePercent;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private InvoicePaymentStatus paymentStatus;
    private String pdfDownloadUrl;
    private Instant pdfGeneratedAt;
    private String notes;

    private UUID salesOrderId;
    private String salesOrderNumber;

    private UUID franchiseId;
    private String franchiseCode;
    private String franchiseBusinessName;
    private String franchiseOwnerName;
    private String franchiseMobile;

    private List<InvoiceItemResponseDto> items;
    private List<PaymentCollectionResponseDto> payments;

    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
