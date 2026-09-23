package com.asalkartechworks.daddysodahouse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Application status response details")
public class StatusResponseDto {

    @Schema(description = "Name of the application", example = "Daddy Soda House Backend")
    private String application;

    @Schema(description = "Current operational status", example = "UP")
    private String status;
}
