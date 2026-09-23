package com.asalkartechworks.daddysodahouse.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to update active status")
public class StatusUpdateRequestDto {

    @NotNull(message = "Active status is mandatory")
    @Schema(description = "Target active status (true to activate, false to deactivate)", example = "false")
    private Boolean active;
}
