package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseCancelRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseItemRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseItemResponseDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseResponseDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseSummaryDto;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.PurchaseEntry;
import com.asalkartechworks.daddysodahouse.entity.PurchaseItem;
import com.asalkartechworks.daddysodahouse.entity.PurchaseStatus;
import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
import com.asalkartechworks.daddysodahouse.entity.Supplier;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseEntryRepository;
import com.asalkartechworks.daddysodahouse.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseEntryRepository purchaseEntryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    /**
     * Create a confirmed purchase bill, increase stock for each item, and record audit movements.
     */
    @Transactional
    public PurchaseResponseDto createPurchase(PurchaseCreateRequestDto dto, UUID userId) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", dto.getSupplierId()));

        if (!supplier.isActive()) {
            throw new BadRequestException("Cannot create purchase under inactive supplier: " + supplier.getSupplierName());
        }

        if (dto.getBillDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Bill date cannot be in the future: " + dto.getBillDate());
        }

        if (StringUtils.hasText(dto.getSupplierBillNumber())) {
            String trimmedBillNo = dto.getSupplierBillNumber().trim();
            if (purchaseEntryRepository.existsBySupplierIdAndSupplierBillNumberAndStatus(
                    supplier.getId(), trimmedBillNo, PurchaseStatus.CONFIRMED)) {
                throw new BadRequestException("Confirmed purchase bill already exists for this supplier with bill number: " + trimmedBillNo);
            }
        }

        // Validate products and check for duplicate product IDs in the request
        Set<UUID> seenProductIds = new HashSet<>();
        List<Product> products = new ArrayList<>();
        for (PurchaseItemRequestDto itemDto : dto.getItems()) {
            if (!seenProductIds.add(itemDto.getProductId())) {
                throw new BadRequestException("Duplicate product in purchase bill: " + itemDto.getProductId());
            }

            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemDto.getProductId()));

            if (!product.isActive()) {
                throw new BadRequestException("Cannot add inactive product to purchase bill: " + product.getFlavourName());
            }

            products.add(product);
        }

        String purchaseNumber = generatePurchaseNumber();

        PurchaseEntry purchaseEntry = PurchaseEntry.builder()
                .purchaseNumber(purchaseNumber)
                .supplier(supplier)
                .supplierBillNumber(StringUtils.hasText(dto.getSupplierBillNumber()) ? dto.getSupplierBillNumber().trim() : null)
                .billDate(dto.getBillDate())
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .status(PurchaseStatus.CONFIRMED)
                .build();

        PurchaseEntry savedPurchase = purchaseEntryRepository.save(purchaseEntry);

        List<PurchaseItemResponseDto> itemResponses = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO;

        for (int i = 0; i < dto.getItems().size(); i++) {
            PurchaseItemRequestDto itemDto = dto.getItems().get(i);
            Product product = products.get(i);

            PurchaseItem purchaseItem = PurchaseItem.builder()
                    .purchaseEntry(savedPurchase)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .build();
            savedPurchase.addItem(purchaseItem);

            // Increase warehouse stock and create PURCHASE_IN movement
            BigDecimal newStockBalance = inventoryService.increaseStock(
                    product,
                    itemDto.getQuantity(),
                    StockMovementType.PURCHASE_IN,
                    "PURCHASE_ENTRY",
                    savedPurchase.getId(),
                    savedPurchase.getPurchaseNumber(),
                    "Stock inward via purchase bill: " + savedPurchase.getPurchaseNumber(),
                    userId
            );

            totalQty = totalQty.add(itemDto.getQuantity());

            itemResponses.add(PurchaseItemResponseDto.builder()
                    .id(purchaseItem.getId())
                    .productId(product.getId())
                    .itemCode(product.getItemCode())
                    .flavourName(product.getFlavourName())
                    .bottleSizeLitre(product.getBottleSizeLitre())
                    .quantity(itemDto.getQuantity())
                    .currentStockBalance(newStockBalance)
                    .build());
        }

        purchaseEntryRepository.save(savedPurchase);
        log.info("Created purchase bill: {} with {} items and total quantity: {}",
                savedPurchase.getPurchaseNumber(), itemResponses.size(), totalQty);

        return toResponseDto(savedPurchase, itemResponses, totalQty);
    }

    /**
     * Cancel a confirmed purchase bill, reducing inventory stock and creating reversal movements.
     */
    @Transactional
    public PurchaseResponseDto cancelPurchase(UUID id, PurchaseCancelRequestDto dto, UUID userId) {
        PurchaseEntry purchase = purchaseEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseEntry", "id", id));

        if (purchase.getStatus() != PurchaseStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED purchase bills can be cancelled. Current status: " + purchase.getStatus());
        }

        if (!StringUtils.hasText(dto.getReason())) {
            throw new BadRequestException("Cancellation reason is mandatory");
        }

        // Pre-flight check: ensure every product has enough stock before performing any reversal
        for (PurchaseItem item : purchase.getItems()) {
            BigDecimal currentStock = inventoryService.getCurrentStock(item.getProduct().getId());
            if (currentStock.compareTo(item.getQuantity()) < 0) {
                throw new BadRequestException(String.format(
                        "Cannot cancel purchase bill %s: Insufficient stock for %s (%s). Current: %s, Required reversal: %s",
                        purchase.getPurchaseNumber(),
                        item.getProduct().getFlavourName(),
                        item.getProduct().getItemCode(),
                        currentStock,
                        item.getQuantity()
                ));
            }
        }

        // Decrement stock and log PURCHASE_CANCEL_REVERSAL movements
        List<PurchaseItemResponseDto> itemResponses = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO;

        for (PurchaseItem item : purchase.getItems()) {
            BigDecimal newBalance = inventoryService.decreaseStock(
                    item.getProduct(),
                    item.getQuantity(),
                    StockMovementType.PURCHASE_CANCEL_REVERSAL,
                    "PURCHASE_ENTRY",
                    purchase.getId(),
                    purchase.getPurchaseNumber(),
                    "Purchase cancellation reversal: " + dto.getReason().trim(),
                    userId
            );

            totalQty = totalQty.add(item.getQuantity());

            itemResponses.add(PurchaseItemResponseDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .itemCode(item.getProduct().getItemCode())
                    .flavourName(item.getProduct().getFlavourName())
                    .bottleSizeLitre(item.getProduct().getBottleSizeLitre())
                    .quantity(item.getQuantity())
                    .currentStockBalance(newBalance)
                    .build());
        }

        purchase.setStatus(PurchaseStatus.CANCELLED);
        purchase.setCancelledReason(dto.getReason().trim());
        purchase.setCancelledAt(Instant.now());
        purchase.setCancelledBy(userId);

        PurchaseEntry updated = purchaseEntryRepository.save(purchase);
        log.info("Cancelled purchase bill: {} with reason: {}", updated.getPurchaseNumber(), dto.getReason());

        return toResponseDto(updated, itemResponses, totalQty);
    }

    /**
     * Get full purchase bill by ID.
     */
    @Transactional(readOnly = true)
    public PurchaseResponseDto getPurchaseById(UUID id) {
        PurchaseEntry purchase = purchaseEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseEntry", "id", id));

        BigDecimal totalQty = BigDecimal.ZERO;
        List<PurchaseItemResponseDto> itemDtos = new ArrayList<>();
        for (PurchaseItem item : purchase.getItems()) {
            totalQty = totalQty.add(item.getQuantity());
            BigDecimal currentStock = inventoryService.getCurrentStock(item.getProduct().getId());
            itemDtos.add(PurchaseItemResponseDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .itemCode(item.getProduct().getItemCode())
                    .flavourName(item.getProduct().getFlavourName())
                    .bottleSizeLitre(item.getProduct().getBottleSizeLitre())
                    .quantity(item.getQuantity())
                    .currentStockBalance(currentStock)
                    .build());
        }

        return toResponseDto(purchase, itemDtos, totalQty);
    }

    /**
     * Search paginated purchases.
     */
    @Transactional(readOnly = true)
    public PageResponse<PurchaseSummaryDto> getAllPurchases(
            UUID supplierId,
            PurchaseStatus status,
            String purchaseNumber,
            String supplierBillNumber,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        Page<PurchaseEntry> page = purchaseEntryRepository.searchPurchases(
                supplierId, status, purchaseNumber, supplierBillNumber, fromDate, toDate, pageable);

        return PageResponse.from(page.map(this::toSummaryDto));
    }

    private synchronized String generatePurchaseNumber() {
        LocalDate now = LocalDate.now();
        String prefix = String.format("PUR-%d%02d-", now.getYear(), now.getMonthValue());

        return purchaseEntryRepository.findMaxPurchaseNumberWithPrefix(prefix)
                .map(maxNumber -> {
                    try {
                        String seqStr = maxNumber.substring(prefix.length());
                        long seq = Long.parseLong(seqStr) + 1;
                        return String.format("%s%05d", prefix, seq);
                    } catch (Exception e) {
                        return String.format("%s%05d", prefix, 1);
                    }
                })
                .orElse(String.format("%s%05d", prefix, 1));
    }

    private PurchaseResponseDto toResponseDto(PurchaseEntry p, List<PurchaseItemResponseDto> items, BigDecimal totalQty) {
        return PurchaseResponseDto.builder()
                .id(p.getId())
                .purchaseNumber(p.getPurchaseNumber())
                .supplierId(p.getSupplier().getId())
                .supplierCode(p.getSupplier().getSupplierCode())
                .supplierName(p.getSupplier().getSupplierName())
                .supplierBillNumber(p.getSupplierBillNumber())
                .billDate(p.getBillDate())
                .description(p.getDescription())
                .status(p.getStatus())
                .cancelledReason(p.getCancelledReason())
                .cancelledAt(p.getCancelledAt())
                .cancelledBy(p.getCancelledBy())
                .items(items)
                .totalQuantity(totalQty)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .createdBy(p.getCreatedBy())
                .updatedBy(p.getUpdatedBy())
                .build();
    }

    private PurchaseSummaryDto toSummaryDto(PurchaseEntry p) {
        BigDecimal totalQty = p.getItems().stream()
                .map(PurchaseItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PurchaseSummaryDto.builder()
                .id(p.getId())
                .purchaseNumber(p.getPurchaseNumber())
                .supplierId(p.getSupplier().getId())
                .supplierCode(p.getSupplier().getSupplierCode())
                .supplierName(p.getSupplier().getSupplierName())
                .supplierBillNumber(p.getSupplierBillNumber())
                .billDate(p.getBillDate())
                .status(p.getStatus())
                .itemCount(p.getItems().size())
                .totalQuantity(totalQty)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
