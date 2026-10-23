package com.asalkartechworks.daddysodahouse.dto.dashboard;

import com.asalkartechworks.daddysodahouse.dto.invoice.InvoiceSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Summary DTO scoped to an individual franchise owner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Franchise owner personalized dashboard summary")
public class FranchiseDashboardSummaryResponseDto {

    private UUID franchiseId;
    private String franchiseName;
    private long activeOutletCount;
    private long machineCount;

    @Schema(description = "Distribution of sales orders by status")
    private Map<String, Long> ordersByStatus;

    private BigDecimal invoiceTotalThisMonth;
    private BigDecimal pendingAmount;
    private BigDecimal availableCredit;
    private BigDecimal overdueAmount;
    private long overdueInvoiceCount;

    @Schema(description = "Earliest upcoming or overdue due invoice, if any")
    private DueInvoiceResponseDto nextDueInvoice;

    @Schema(description = "Latest five sales orders")
    private List<OrderSummaryDto> latestFiveOrders;

    @Schema(description = "Latest five invoices")
    private List<InvoiceSummaryDto> latestFiveInvoices;
}
