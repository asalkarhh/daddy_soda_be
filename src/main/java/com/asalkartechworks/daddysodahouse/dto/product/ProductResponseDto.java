package com.asalkartechworks.daddysodahouse.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product details response")
public class ProductResponseDto {

    @Schema(description = "Product UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Unique item/product code", example = "SYR-ORG-05L")
    private String itemCode;

    @Schema(description = "Flavour name", example = "Orange Flavour Syrup")
    private String flavourName;

    @Schema(description = "Bottle capacity volume in litres", example = "5.00")
    private BigDecimal bottleSizeLitre;

    @Schema(description = "Standard selling price per bottle", example = "650.00")
    private BigDecimal defaultSellingPrice;

    @Schema(description = "Product description", example = "Premium concentrated orange syrup bottle for 8-valve dispenser")
    private String description;

    @Schema(description = "Whether product is active", example = "true")
    private boolean active;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
