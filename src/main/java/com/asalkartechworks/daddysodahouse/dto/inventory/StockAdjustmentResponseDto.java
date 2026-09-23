package com.asalkartechworks.daddysodahouse.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentResponseDto {

    private UUID productId;
    private String itemCode;
    private String flavourName;
    private StockAdjustmentType adjustmentType;
    private BigDecimal quantity;
    private BigDecimal stockBefore;
    private BigDecimal stockAfter;
    private String note;
    private Instant adjustmentAt;
}
