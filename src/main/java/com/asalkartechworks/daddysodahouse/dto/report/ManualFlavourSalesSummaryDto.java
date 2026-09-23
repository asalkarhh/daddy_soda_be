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
public class ManualFlavourSalesSummaryDto {

    private UUID productId;
    private String itemCode;
    private String flavourName;
    private BigDecimal glassesTotal;
    private BigDecimal mlTotal;
}
