package com.asalkartechworks.daddysodahouse.dto.purchase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseCancelRequestDto {

    @NotBlank(message = "Cancellation reason is mandatory")
    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    private String reason;
}
