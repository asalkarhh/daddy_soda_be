package com.asalkartechworks.daddysodahouse.dto.order;

import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
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
public class OrderSummaryDto {

    private UUID id;
    private String orderNumber;
    private UUID franchiseId;
    private String franchiseCode;
    private String franchiseBusinessName;
    private LocalDate orderDate;
    private SalesOrderStatus status;
    private String updateReason;
    private int itemCount;
    private BigDecimal totalQuantity;
    private BigDecimal orderTotal;
    private UUID createdBy;
    private Instant createdAt;
}
