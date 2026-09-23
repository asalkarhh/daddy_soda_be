package com.asalkartechworks.daddysodahouse.dto.invoice;

import com.asalkartechworks.daddysodahouse.entity.InvoiceType;
import com.asalkartechworks.daddysodahouse.entity.PaymentTerm;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreateRequestDto {

    @NotNull(message = "Sales order ID is mandatory")
    private UUID salesOrderId;

    @NotNull(message = "Invoice type is mandatory (GST or NORMAL)")
    private InvoiceType invoiceType;

    @PastOrPresent(message = "Invoice date cannot be in the future")
    private LocalDate invoiceDate;

    @NotNull(message = "Payment term is mandatory (FULL_PAYMENT, PARTIAL_PAYMENT, or CREDIT)")
    private PaymentTerm paymentTerm;

    private LocalDate dueDate;

    @DecimalMin(value = "0.00", message = "GST rate percent must be 0 or greater")
    @DecimalMax(value = "100.00", message = "GST rate percent cannot exceed 100")
    private BigDecimal gstRatePercent;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
