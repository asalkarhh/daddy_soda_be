package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.product.ProductRequestDto;
import com.asalkartechworks.daddysodahouse.dto.product.ProductResponseDto;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto dto) {
        if (productRepository.existsByItemCode(dto.getItemCode())) {
            throw new BadRequestException("Item code already exists: " + dto.getItemCode());
        }

        validateProductPricingAndSize(dto.getBottleSizeLitre(), dto.getDefaultSellingPrice());

        Product product = Product.builder()
                .itemCode(dto.getItemCode().trim())
                .flavourName(dto.getFlavourName().trim())
                .bottleSizeLitre(dto.getBottleSizeLitre())
                .defaultSellingPrice(dto.getDefaultSellingPrice())
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .isActive(true)
                .build();

        Product saved = productRepository.save(product);
        log.info("Created product: {} with ID: {}", saved.getItemCode(), saved.getId());
        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(UUID id) {
        return toResponseDto(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponseDto> getAllProducts(String search, Boolean active, Pageable pageable) {
        Page<Product> page = productRepository.searchProducts(search, active, pageable);
        return PageResponse.from(page.map(this::toResponseDto));
    }

    @Transactional
    public ProductResponseDto updateProduct(UUID id, ProductRequestDto dto) {
        Product product = findEntityById(id);

        if (!product.getItemCode().equalsIgnoreCase(dto.getItemCode().trim())
                && productRepository.existsByItemCode(dto.getItemCode())) {
            throw new BadRequestException("Item code already exists: " + dto.getItemCode());
        }

        validateProductPricingAndSize(dto.getBottleSizeLitre(), dto.getDefaultSellingPrice());

        product.setItemCode(dto.getItemCode().trim());
        product.setFlavourName(dto.getFlavourName().trim());
        product.setBottleSizeLitre(dto.getBottleSizeLitre());
        product.setDefaultSellingPrice(dto.getDefaultSellingPrice());
        product.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);

        Product updated = productRepository.save(product);
        log.info("Updated product ID: {}", updated.getId());
        return toResponseDto(updated);
    }

    @Transactional
    public ProductResponseDto updateStatus(UUID id, boolean active) {
        Product product = findEntityById(id);
        product.setActive(active);
        Product updated = productRepository.save(product);
        log.info("Product ID: {} status updated to active={}", updated.getId(), active);
        return toResponseDto(updated);
    }

    private void validateProductPricingAndSize(BigDecimal bottleSizeLitre, BigDecimal defaultSellingPrice) {
        if (bottleSizeLitre == null || bottleSizeLitre.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Bottle size in litres must be greater than zero");
        }
        if (defaultSellingPrice != null && defaultSellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Default selling price cannot be negative");
        }
    }

    public Product findEntityById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    public ProductResponseDto toResponseDto(Product p) {
        return ProductResponseDto.builder()
                .id(p.getId())
                .itemCode(p.getItemCode())
                .flavourName(p.getFlavourName())
                .bottleSizeLitre(p.getBottleSizeLitre())
                .defaultSellingPrice(p.getDefaultSellingPrice())
                .description(p.getDescription())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
