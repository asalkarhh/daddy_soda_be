package com.asalkartechworks.daddysodahouse.dto.iot;

import com.asalkartechworks.daddysodahouse.entity.IotItemType;
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
public class IotEventItemResponseDto {

    private UUID id;
    private UUID productId;
    private IotItemType itemType;
    private String itemCode;
    private String itemName;
    private Integer lineNo;
    private BigDecimal quantityMl;
}
