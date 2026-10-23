package com.asalkartechworks.daddysodahouse.dto.order;

import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private UUID id;
    private String orderNumber;
    private UUID franchiseId;
    private String franchiseCode;
    private String franchiseBusinessName;
    private String franchiseOwnerName;
    private String franchiseMobile;
    private LocalDate orderDate;
    private SalesOrderStatus status;
    private String notes;
    private String updateReason;
    private String reorderedFromOrderNumber;
    private String cancellationReason;
    private Instant cancelledAt;
    private UUID cancelledBy;
    private String cancelledByRole;
    private Instant preparedAt;
    private UUID preparedBy;
    private Instant deliveredAt;
    private UUID deliveredBy;
    private List<OrderItemResponseDto> items;
    private BigDecimal totalQuantity;
    private BigDecimal orderTotal;
    private BigDecimal availableCredit;
    private BigDecimal estimatedCreditApplied;
    private BigDecimal estimatedPayableAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
