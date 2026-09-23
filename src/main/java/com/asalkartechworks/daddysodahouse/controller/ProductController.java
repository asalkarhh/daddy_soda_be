package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.common.StatusUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.product.ProductRequestDto;
import com.asalkartechworks.daddysodahouse.dto.product.ProductResponseDto;
import com.asalkartechworks.daddysodahouse.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
@Tag(name = "Product Master", description = "Syrup/flavour product catalog management (SUPER_ADMIN and MANAGER)")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create product", description = "Create a syrup/flavour product item")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(@Valid @RequestBody ProductRequestDto dto) {
        ProductResponseDto response = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Product created successfully"));
    }

    @GetMapping
    @Operation(summary = "List products", description = "Get paginated products with search and active status filters")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDto>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ProductResponseDto> response = productService.getAllProducts(search, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Products retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve single product details")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable UUID id) {
        ProductResponseDto response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Product retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update details of an existing product")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDto dto) {
        ProductResponseDto response = productService.updateProduct(id, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Product updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update product status", description = "Activate or deactivate a product")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequestDto dto) {
        ProductResponseDto response = productService.updateStatus(id, dto.getActive());
        return ResponseEntity.ok(ApiResponse.success(response, "Product status updated successfully"));
    }
}
