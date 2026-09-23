package com.asalkartechworks.daddysodahouse.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class StockAdjustmentRequestDto {

    @NotNull(message = "Product ID is mandatory")
    private UUID productId;

    @NotNull(message = "Adjustment type is mandatory")
    private StockAdjustmentType adjustmentType;

    @NotNull(message = "Quantity is mandatory")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotBlank(message = "Adjustment note is mandatory")
    @Size(max = 500, message = "Adjustment note cannot exceed 500 characters")
    private String note;
}
