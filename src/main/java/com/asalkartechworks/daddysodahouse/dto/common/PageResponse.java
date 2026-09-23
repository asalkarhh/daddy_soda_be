package com.asalkartechworks.daddysodahouse.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated response envelope.
 *
 * @param <T> element type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard paginated list container")
public class PageResponse<T> {

    @Schema(description = "List of records for the current page")
    private List<T> content;

    @Schema(description = "Current page index (0-based)", example = "0")
    private int page;

    @Schema(description = "Number of records requested per page", example = "10")
    private int size;

    @Schema(description = "Total number of records matching the query", example = "42")
    private long totalElements;

    @Schema(description = "Total number of available pages", example = "5")
    private int totalPages;

    public static <T> PageResponse<T> from(Page<T> springPage) {
        return PageResponse.<T>builder()
                .content(springPage.getContent())
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .build();
    }
}
