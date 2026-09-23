package com.asalkartechworks.daddysodahouse.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineConsumptionDto {

    private String machineCode;
    private String machineName;
    private BigDecimal syrupUsageMl;
    private BigDecimal rawSodaUsageMl;
}
