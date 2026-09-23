package com.asalkartechworks.daddysodahouse.dto.order;

import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

    private UUID franchiseId;

    @PastOrPresent(message = "Order date cannot be a future date")
    private LocalDate orderDate;

    @Builder.Default
    private SalesOrderStatus status = SalesOrderStatus.PLACED;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "Order items list cannot be empty; minimum one item is required")
    @Valid
    private List<OrderItemRequestDto> items;
}
