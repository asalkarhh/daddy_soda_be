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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSummaryDto {

    private UUID id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private UUID franchiseId;
    private String franchiseCode;
    private String franchiseBusinessName;
    private UUID salesOrderId;
    private String salesOrderNumber;
    private InvoiceType invoiceType;
    private PaymentTerm paymentTerm;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private InvoicePaymentStatus paymentStatus;
    private boolean pdfAvailable;
    private Instant createdAt;
}
