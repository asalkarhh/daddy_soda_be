package com.asalkartechworks.daddysodahouse.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionSummaryResponseDto {

    private BigDecimal totalSyrupUsageMl;
    private BigDecimal totalRawSodaUsageMl;
    private BigDecimal manualQuantityTotal;
    private List<FlavourConsumptionDto> flavourWiseIotUsage;
    private List<MachineConsumptionDto> machineWiseIotUsage;
    private List<ManualFlavourSalesSummaryDto> manualFlavourSalesTotals;
    private String sourceNotes;
}
