package com.asalkartechworks.daddysodahouse.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderUpdateRequestDto {

    private UUID franchiseId;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    private String reason;

    @NotEmpty(message = "Order items list cannot be empty; minimum one item is required")
    @Valid
    private List<OrderItemRequestDto> items;
}
