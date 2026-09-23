package com.asalkartechworks.daddysodahouse.dto.manualsales;

import com.asalkartechworks.daddysodahouse.entity.ManualSalesQuantityUnit;
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
public class ManualDailySalesResponseDto {

    private UUID id;
    private UUID franchiseId;
    private String franchiseName;
    private UUID machineId;
    private String machineCode;
    private String machineName;
    private UUID productId;
    private String productCode;
    private String flavourName;
    private LocalDate salesDate;
    private BigDecimal quantity;
    private ManualSalesQuantityUnit quantityUnit;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
}
