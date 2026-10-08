package com.asalkartechworks.daddysodahouse.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class OrderUpdateRequestDto {

    private UUID franchiseId;

    private LocalDate orderDate;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    @Size(max = 1000, message = "Update reason cannot exceed 1000 characters")
    private String updateReason;

    @NotEmpty(message = "Order items list cannot be empty; minimum one item is required")
    @Valid
    private List<OrderItemRequestDto> items;
}
