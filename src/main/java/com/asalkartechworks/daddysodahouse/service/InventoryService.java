package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.inventory.InventoryProductDetailDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.InventoryStockResponseDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockAdjustmentRequestDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockAdjustmentResponseDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockAdjustmentType;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockMovementResponseDto;
import com.asalkartechworks.daddysodahouse.dto.inventory.StockStatusFilter;
import com.asalkartechworks.daddysodahouse.entity.InventoryStock;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.StockMovement;
import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.InventoryStockRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryStockRepository inventoryStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    /**
     * Increase stock for a product using pessimistic locking.
     * Creates inventory_stock automatically if it does not yet exist.
     */
    @Transactional
    public BigDecimal increaseStock(Product product,
                                  BigDecimal quantity,
                                  StockMovementType movementType,
                                  String referenceType,
                                  UUID referenceId,
                                  String referenceNumber,
                                  String note,
                                  UUID userId) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity to increase must be greater than zero");
        }

        InventoryStock stock = inventoryStockRepository.findByProductIdWithLock(product.getId())
                .orElseGet(() -> {
                    InventoryStock newStock = InventoryStock.builder()
                            .product(product)
                            .availableQuantity(BigDecimal.ZERO)
                            .build();
                    return inventoryStockRepository.save(newStock);
                });

        BigDecimal oldBalance = stock.getAvailableQuantity();
        BigDecimal newBalance = oldBalance.add(quantity);
        stock.setAvailableQuantity(newBalance);
        inventoryStockRepository.save(stock);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType(movementType)
                .quantityIn(quantity)
                .quantityOut(BigDecimal.ZERO)
                .balanceAfter(newBalance)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .note(note)
                .movementAt(Instant.now())
                .createdBy(userId)
                .build();
        stockMovementRepository.save(movement);

        log.info("Stock increased for product: {} ({}) by {}. Old: {}, New: {}",
                product.getItemCode(), product.getId(), quantity, oldBalance, newBalance);
        return newBalance;
    }

    /**
     * Decrease stock for a product using pessimistic locking.
     * Prevents stock from becoming negative.
     */
    @Transactional
    public BigDecimal decreaseStock(Product product,
                                  BigDecimal quantity,
                                  StockMovementType movementType,
                                  String referenceType,
                                  UUID referenceId,
                                  String referenceNumber,
                                  String note,
                                  UUID userId) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity to decrease must be greater than zero");
        }

        InventoryStock stock = inventoryStockRepository.findByProductIdWithLock(product.getId())
                .orElseGet(() -> {
                    InventoryStock newStock = InventoryStock.builder()
                            .product(product)
                            .availableQuantity(BigDecimal.ZERO)
                            .build();
                    return inventoryStockRepository.save(newStock);
                });

        BigDecimal currentBalance = stock.getAvailableQuantity();
        if (currentBalance.compareTo(quantity) < 0) {
            throw new BadRequestException(String.format(
                    "Insufficient inventory stock for %s (%s). Current: %s, Requested reduction: %s",
                    product.getFlavourName(), product.getItemCode(), currentBalance, quantity
            ));
        }

        BigDecimal newBalance = currentBalance.subtract(quantity);
        stock.setAvailableQuantity(newBalance);
        inventoryStockRepository.save(stock);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType(movementType)
                .quantityIn(BigDecimal.ZERO)
                .quantityOut(quantity)
                .balanceAfter(newBalance)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .note(note)
                .movementAt(Instant.now())
                .createdBy(userId)
                .build();
        stockMovementRepository.save(movement);

        log.info("Stock decreased for product: {} ({}) by {}. Old: {}, New: {}",
                product.getItemCode(), product.getId(), quantity, currentBalance, newBalance);
        return newBalance;
    }

    /**
     * Perform a manual stock adjustment (ADJUSTMENT_IN or ADJUSTMENT_OUT).
     */
    @Transactional
    public StockAdjustmentResponseDto adjustStock(StockAdjustmentRequestDto dto, UUID userId) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", dto.getProductId()));

        BigDecimal stockBefore = inventoryStockRepository.findByProductId(product.getId())
                .map(InventoryStock::getAvailableQuantity)
                .orElse(BigDecimal.ZERO);

        BigDecimal stockAfter;
        if (dto.getAdjustmentType() == StockAdjustmentType.ADJUSTMENT_IN) {
            stockAfter = increaseStock(
                    product,
                    dto.getQuantity(),
                    StockMovementType.ADJUSTMENT_IN,
                    "MANUAL_ADJUSTMENT",
                    null,
                    "ADJ-" + System.currentTimeMillis(),
                    dto.getNote(),
                    userId
            );
        } else {
            stockAfter = decreaseStock(
                    product,
                    dto.getQuantity(),
                    StockMovementType.ADJUSTMENT_OUT,
                    "MANUAL_ADJUSTMENT",
                    null,
                    "ADJ-" + System.currentTimeMillis(),
                    dto.getNote(),
                    userId
            );
        }

        return StockAdjustmentResponseDto.builder()
                .productId(product.getId())
                .itemCode(product.getItemCode())
                .flavourName(product.getFlavourName())
                .adjustmentType(dto.getAdjustmentType())
                .quantity(dto.getQuantity())
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .note(dto.getNote())
                .adjustmentAt(Instant.now())
                .build();
    }

    /**
     * Paginated list of current warehouse stock.
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryStockResponseDto> getWarehouseStock(
            UUID productId,
            String search,
            StockStatusFilter stockStatus,
            Boolean active,
            Pageable pageable) {

        String statusStr = stockStatus != null ? stockStatus.name() : null;
        Page<Product> products = productRepository.searchInventoryProducts(productId, search, active, statusStr, pageable);

        Page<InventoryStockResponseDto> dtoPage = products.map(product -> {
            BigDecimal qty = inventoryStockRepository.findByProductId(product.getId())
                    .map(InventoryStock::getAvailableQuantity)
                    .orElse(BigDecimal.ZERO);
            Instant lastMovement = stockMovementRepository.findLatestMovementAtByProductId(product.getId())
                    .orElse(null);

            return InventoryStockResponseDto.builder()
                    .productId(product.getId())
                    .itemCode(product.getItemCode())
                    .flavourName(product.getFlavourName())
                    .bottleSizeLitre(product.getBottleSizeLitre())
                    .availableQuantity(qty)
                    .active(product.isActive())
                    .lastMovementAt(lastMovement)
                    .build();
        });

        return PageResponse.from(dtoPage);
    }

    /**
     * Get stock summary and most recent 20 stock movements for a specific product.
     */
    @Transactional(readOnly = true)
    public InventoryProductDetailDto getProductInventoryDetail(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        BigDecimal qty = inventoryStockRepository.findByProductId(product.getId())
                .map(InventoryStock::getAvailableQuantity)
                .orElse(BigDecimal.ZERO);

        Instant lastMovement = stockMovementRepository.findLatestMovementAtByProductId(product.getId())
                .orElse(null);

        List<StockMovement> recentMovements = stockMovementRepository
                .findTop20ByProductIdOrderByMovementAtDescCreatedAtDesc(productId);

        List<StockMovementResponseDto> movementDtos = recentMovements.stream()
                .map(this::toMovementResponseDto)
                .toList();

        return InventoryProductDetailDto.builder()
                .productId(product.getId())
                .itemCode(product.getItemCode())
                .flavourName(product.getFlavourName())
                .bottleSizeLitre(product.getBottleSizeLitre())
                .availableQuantity(qty)
                .active(product.isActive())
                .lastMovementAt(lastMovement)
                .recentMovements(movementDtos)
                .build();
    }

    /**
     * Paginated list of stock movements across products and filters.
     */
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponseDto> getStockMovements(
            UUID productId,
            StockMovementType movementType,
            String referenceType,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        Instant fromInstant = fromDate != null ? fromDate.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant toInstant = toDate != null ? toDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC) : null;

        Specification<StockMovement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (movementType != null) {
                predicates.add(cb.equal(root.get("movementType"), movementType));
            }
            if (StringUtils.hasText(referenceType)) {
                predicates.add(cb.equal(cb.lower(root.get("referenceType")), referenceType.trim().toLowerCase()));
            }
            if (fromInstant != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("movementAt"), fromInstant));
            }
            if (toInstant != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("movementAt"), toInstant));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockMovement> page = stockMovementRepository.findAll(spec, pageable);


        return PageResponse.from(page.map(this::toMovementResponseDto));
    }

    /**
     * Current stock balance for a product.
     */
    @Transactional(readOnly = true)
    public BigDecimal getCurrentStock(UUID productId) {
        return inventoryStockRepository.findByProductId(productId)
                .map(InventoryStock::getAvailableQuantity)
                .orElse(BigDecimal.ZERO);
    }

    public StockMovementResponseDto toMovementResponseDto(StockMovement m) {
        return StockMovementResponseDto.builder()
                .id(m.getId())
                .productId(m.getProduct().getId())
                .itemCode(m.getProduct().getItemCode())
                .flavourName(m.getProduct().getFlavourName())
                .movementType(m.getMovementType())
                .quantityIn(m.getQuantityIn())
                .quantityOut(m.getQuantityOut())
                .balanceAfter(m.getBalanceAfter())
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .referenceNumber(m.getReferenceNumber())
                .note(m.getNote())
                .movementAt(m.getMovementAt())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
