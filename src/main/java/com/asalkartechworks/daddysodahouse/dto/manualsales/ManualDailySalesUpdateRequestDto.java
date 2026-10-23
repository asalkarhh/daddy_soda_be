package com.asalkartechworks.daddysodahouse.dto.manualsales;

import com.asalkartechworks.daddysodahouse.entity.ManualSalesQuantityUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualDailySalesUpdateRequestDto {

    @NotNull(message = "Quantity is mandatory")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "Quantity unit is mandatory")
    private ManualSalesQuantityUnit quantityUnit;

    @DecimalMin(value = "0.00", message = "Retail unit price cannot be negative")
    private BigDecimal retailUnitPrice;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
