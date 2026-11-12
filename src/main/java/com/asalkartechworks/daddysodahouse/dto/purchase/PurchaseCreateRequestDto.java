package com.asalkartechworks.daddysodahouse.dto.purchase;

import com.asalkartechworks.daddysodahouse.entity.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseCreateRequestDto {

    @NotNull(message = "Supplier ID is mandatory")
    private UUID supplierId;

    @Size(max = 100, message = "Supplier bill number cannot exceed 100 characters")
    private String supplierBillNumber;

    @NotNull(message = "Bill date is mandatory")
    @PastOrPresent(message = "Bill date cannot be a future date")
    private LocalDate billDate;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotEmpty(message = "Items list cannot be empty; minimum one item is required")
    @Valid
    private List<PurchaseItemRequestDto> items;

    @DecimalMin(value = "0.00", message = "GST rate percent cannot be negative")
    private BigDecimal gstRatePercent;

    @DecimalMin(value = "0.00", message = "Paid amount cannot be negative")
    private BigDecimal paidAmount;

    private PaymentMode paymentMode;

    @Size(max = 100, message = "Payment reference cannot exceed 100 characters")
    private String paymentReference;

    @Size(max = 500, message = "Payment remarks cannot exceed 500 characters")
    private String paymentRemarks;
}
