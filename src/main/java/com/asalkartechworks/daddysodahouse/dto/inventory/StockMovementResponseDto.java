package com.asalkartechworks.daddysodahouse.dto.inventory;

import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
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
public class StockMovementResponseDto {

    private UUID id;
    private UUID productId;
    private String itemCode;
    private String flavourName;
    private StockMovementType movementType;
    private BigDecimal quantityIn;
    private BigDecimal quantityOut;
    private BigDecimal balanceAfter;
    private String referenceType;
    private UUID referenceId;
    private String referenceNumber;
    private String note;
    private Instant movementAt;
    private Instant createdAt;
}
