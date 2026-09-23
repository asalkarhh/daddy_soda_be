package com.asalkartechworks.daddysodahouse.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Summary DTO containing high-level operational and business metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Overall business and operations dashboard summary")
public class DashboardSummaryResponseDto {

    private long activeFranchises;
    private long activeOutlets;
    private long activeMachines;
    private long activeProducts;

    private long warehouseProductsInStock;
    private long warehouseProductsOutOfStock;

    private long purchasesThisMonthCount;
    private BigDecimal purchaseQuantityThisMonth;

    private long ordersPlacedCount;
    private long ordersPreparedCount;
    private long ordersDeliveredThisMonthCount;

    private long invoicesThisMonthCount;
    private BigDecimal invoiceTotalThisMonth;

    private long pendingInvoiceCount;
    private BigDecimal pendingAmount;

    private long overdueInvoiceCount;
    private BigDecimal overdueAmount;

    private long newEnquiryCount;
}
