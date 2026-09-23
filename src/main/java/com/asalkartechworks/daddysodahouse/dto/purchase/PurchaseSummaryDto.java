package com.asalkartechworks.daddysodahouse.dto.purchase;

import com.asalkartechworks.daddysodahouse.entity.PurchaseStatus;
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
public class PurchaseSummaryDto {

    private UUID id;
    private String purchaseNumber;
    private UUID supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierBillNumber;
    private LocalDate billDate;
    private PurchaseStatus status;
    private int itemCount;
    private BigDecimal totalQuantity;
    private Instant createdAt;
}
