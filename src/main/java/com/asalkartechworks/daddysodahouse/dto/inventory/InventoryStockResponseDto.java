package com.asalkartechworks.daddysodahouse.dto.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class InventoryStockResponseDto {

    private UUID productId;
    private String itemCode;
    private String flavourName;
    private BigDecimal bottleSizeLitre;
    private BigDecimal availableQuantity;
    private boolean active;
    private Instant lastMovementAt;

    @JsonProperty("productActive")
    public boolean getProductActive() {
        return active;
    }
}
