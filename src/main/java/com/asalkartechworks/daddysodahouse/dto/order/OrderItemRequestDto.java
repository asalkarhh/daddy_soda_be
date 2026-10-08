package com.asalkartechworks.daddysodahouse.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class OrderItemRequestDto {

    @NotNull(message = "Product ID is mandatory")
    private UUID productId;

    @NotNull(message = "Quantity is mandatory")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @DecimalMin(value = "0.00", message = "Unit price must be zero or greater")
    private BigDecimal unitPrice;

    private BigDecimal originalQuantity;

    private String adminNote;
}
