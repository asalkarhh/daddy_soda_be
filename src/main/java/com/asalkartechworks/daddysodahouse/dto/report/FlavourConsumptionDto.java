package com.asalkartechworks.daddysodahouse.dto.report;

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
public class FlavourConsumptionDto {

    private UUID productId;
    private String itemCode;
    private String flavourName;
    private BigDecimal syrupUsageMl;
    private BigDecimal usagePercentage;
}
