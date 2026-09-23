package com.asalkartechworks.daddysodahouse.dto.dashboard;

import com.asalkartechworks.daddysodahouse.entity.InvoicePaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing an outstanding invoice due item for dashboard display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Outstanding invoice due item")
public class DueInvoiceResponseDto {

    private UUID invoiceId;
    private String invoiceNumber;
    private UUID franchiseId;
    private String franchiseName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private InvoicePaymentStatus paymentStatus;
    private DueType dueType;
    private long daysOverdue;

    @Schema(description = "Severity color indicator: RED for OVERDUE, AMBER for DUE_TODAY, NORMAL for UPCOMING", example = "RED")
    private DueSeverity displaySeverity;
}
