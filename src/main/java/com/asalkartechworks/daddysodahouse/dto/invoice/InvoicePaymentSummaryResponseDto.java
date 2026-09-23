package com.asalkartechworks.daddysodahouse.dto.invoice;

import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentSummaryResponseDto {

    private PaymentCollectionResponseDto payment;
    private UUID invoiceId;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private InvoicePaymentStatus paymentStatus;
}
