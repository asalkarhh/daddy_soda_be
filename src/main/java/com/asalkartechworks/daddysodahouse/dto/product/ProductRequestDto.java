package com.asalkartechworks.daddysodahouse.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Product flavour/syrup creation/update request payload")
public class ProductRequestDto {

    @NotBlank(message = "Item code is mandatory")
    @Size(max = 50, message = "Item code cannot exceed 50 characters")
    @Schema(description = "Unique item/product code", example = "SYR-ORG-05L")
    private String itemCode;

    @NotBlank(message = "Flavour name is mandatory")
    @Size(max = 150, message = "Flavour name cannot exceed 150 characters")
    @Schema(description = "Name of flavour/syrup", example = "Orange Flavour Syrup")
    private String flavourName;

    @NotNull(message = "Bottle size in litres is mandatory")
    @DecimalMin(value = "0.01", message = "Bottle size in litres must be greater than zero")
    @Schema(description = "Bottle volume capacity in litres (defaults to 5.00)", example = "5.00")
    private BigDecimal bottleSizeLitre;

    @DecimalMin(value = "0.00", message = "Default selling price must be zero or greater")
    @Schema(description = "Standard selling price per bottle (optional)", example = "650.00")
    private BigDecimal defaultSellingPrice;

    @Schema(description = "Product details / flavour notes", example = "Premium concentrated orange syrup bottle for 8-valve dispenser")
    private String description;
}
