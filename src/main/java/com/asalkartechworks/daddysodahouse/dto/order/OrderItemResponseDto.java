package com.asalkartechworks.daddysodahouse.dto.order;

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
public class OrderItemResponseDto {

    private UUID id;
    private UUID productId;
    private String itemCode;
    private String flavourName;
    private BigDecimal bottleSizeLitre;
    private BigDecimal quantity;
    private BigDecimal originalQuantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private String adminNote;
}
