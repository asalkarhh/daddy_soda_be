package com.asalkartechworks.daddysodahouse.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderDispatchRequestDto {
    @NotBlank(message = "Dispatch remark is mandatory")
    @Size(max = 1000, message = "Dispatch remark cannot exceed 1000 characters")
    private String remark;
}
