package com.asalkartechworks.daddysodahouse.dto.iot;

import com.asalkartechworks.daddysodahouse.entity.IotItemType;
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
public class IotEventItemIngestRequestDto {

    @NotNull(message = "Item type is mandatory")
    private IotItemType itemType;

    @NotBlank(message = "Item code is mandatory")
    @Size(max = 50, message = "Item code cannot exceed 50 characters")
    private String itemCode;

    @Size(max = 150, message = "Item name cannot exceed 150 characters")
    private String itemName;

    private Integer lineNo;

    @NotNull(message = "Quantity in ML is mandatory")
    @DecimalMin(value = "0.001", message = "Quantity must be greater than zero")
    private BigDecimal quantityMl;
}
