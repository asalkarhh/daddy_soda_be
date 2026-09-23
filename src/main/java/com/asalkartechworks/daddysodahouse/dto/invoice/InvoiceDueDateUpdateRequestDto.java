package com.asalkartechworks.daddysodahouse.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDueDateUpdateRequestDto {

    @NotNull(message = "New due date is mandatory")
    private LocalDate dueDate;

    @NotBlank(message = "Reason for due date update is mandatory")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
