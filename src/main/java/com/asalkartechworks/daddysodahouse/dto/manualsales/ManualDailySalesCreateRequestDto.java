package com.asalkartechworks.daddysodahouse.dto.manualsales;

import com.asalkartechworks.daddysodahouse.entity.ManualSalesQuantityUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualDailySalesCreateRequestDto {

    /**
     * Required for SUPER_ADMIN and MANAGER; ignored or validated against JWT for FRANCHISE_OWNER.
     */
    private UUID franchiseId;

    @NotNull(message = "Machine ID is mandatory")
    private UUID machineId;

    @NotNull(message = "Product ID is mandatory")
    private UUID productId;

    @NotNull(message = "Sales date is mandatory")
    @PastOrPresent(message = "Sales date cannot be in the future")
    private LocalDate salesDate;

    @NotNull(message = "Quantity is mandatory")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @Builder.Default
    private ManualSalesQuantityUnit quantityUnit = ManualSalesQuantityUnit.GLASSES;

    @DecimalMin(value = "0.00", message = "Retail unit price cannot be negative")
    private BigDecimal retailUnitPrice;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
