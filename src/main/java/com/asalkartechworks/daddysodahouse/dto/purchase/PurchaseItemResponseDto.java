package com.asalkartechworks.daddysodahouse.dto.purchase;

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
public class PurchaseItemResponseDto {

    private UUID id;
    private UUID productId;
    private String itemCode;
    private String flavourName;
    private BigDecimal bottleSizeLitre;
    private BigDecimal quantity;
    private BigDecimal currentStockBalance;
}
