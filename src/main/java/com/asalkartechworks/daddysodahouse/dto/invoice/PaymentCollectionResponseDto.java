package com.asalkartechworks.daddysodahouse.dto.invoice;

import com.asalkartechworks.daddysodahouse.entity.PaymentMode;
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
public class PaymentCollectionResponseDto {

    private UUID id;
    private UUID invoiceId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String referenceNumber;
    private String remarks;
    private Instant createdAt;
    private UUID createdBy;
}
