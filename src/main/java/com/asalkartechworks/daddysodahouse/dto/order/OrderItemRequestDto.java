package com.asalkartechworks.daddysodahouse.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemRequestDto {

    @NotNull(message = "Product ID is mandatory")
    private UUID productId;

    @NotNull(message = "Quantity is mandatory")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private BigDecimal originalQuantity;

    private String adminNote;

    @DecimalMin(value = "0.00", message = "Unit price must be zero or greater")
    private BigDecimal unitPrice;
}
