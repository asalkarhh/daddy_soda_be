package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseCancelRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseItemRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseItemResponseDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchasePaymentRequestDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchasePaymentResponseDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseResponseDto;
import com.asalkartechworks.daddysodahouse.dto.purchase.PurchaseSummaryDto;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.PurchaseEntry;
import com.asalkartechworks.daddysodahouse.entity.PurchaseItem;
import com.asalkartechworks.daddysodahouse.entity.PurchasePayment;
import com.asalkartechworks.daddysodahouse.entity.PurchasePaymentStatus;
import com.asalkartechworks.daddysodahouse.entity.PurchaseStatus;
import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
import com.asalkartechworks.daddysodahouse.entity.Supplier;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchaseEntryRepository;
import com.asalkartechworks.daddysodahouse.repository.PurchasePaymentRepository;
import com.asalkartechworks.daddysodahouse.repository.SupplierRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseEntryRepository purchaseEntryRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    /**
     * Create a confirmed purchase bill with item prices, 18% default GST, total payable, optional initial payment, and stock inward.
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

            if (itemDto.getQuantity() == null || itemDto.getQuantity().compareTo(BigDecimal.ONE) < 0 || itemDto.getQuantity().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("Quantity must be a positive whole number for product: " + product.getFlavourName());
            }

            if (itemDto.getUnitPrice() == null || itemDto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Unit buying price cannot be negative for product: " + product.getFlavourName());
            }

            products.add(product);
        }

        String purchaseNumber = generatePurchaseNumber();

        // Financial calculations (always rounded to clean whole numbers / integer rupees)
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PurchaseItemRequestDto itemDto : dto.getItems()) {
            BigDecimal roundedUnitPrice = itemDto.getUnitPrice().setScale(0, RoundingMode.HALF_UP);
            BigDecimal lineTotal = roundedUnitPrice.multiply(itemDto.getQuantity()).setScale(0, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);
        }

        // Default 18% GST if not explicitly specified
        BigDecimal gstRate = dto.getGstRatePercent() != null ? dto.getGstRatePercent().setScale(0, RoundingMode.HALF_UP) : BigDecimal.valueOf(18);
        BigDecimal gstAmount = subtotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(gstAmount);

        BigDecimal initialPaid = dto.getPaidAmount() != null ? dto.getPaidAmount().setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (initialPaid.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Paid amount cannot be negative");
        }
        if (initialPaid.compareTo(totalAmount) > 0) {
            throw new BadRequestException(String.format(
                    "Initial paid amount %s cannot exceed total bill amount %s", initialPaid, totalAmount));
        }
        if (initialPaid.compareTo(BigDecimal.ZERO) > 0 && dto.getPaymentMode() == null) {
            throw new BadRequestException("Payment mode is mandatory when recording an initial payment");
        }

        BigDecimal balanceAmount = totalAmount.subtract(initialPaid);

        PurchaseEntry purchaseEntry = PurchaseEntry.builder()
                .purchaseNumber(purchaseNumber)
                .supplier(supplier)
                .supplierBillNumber(StringUtils.hasText(dto.getSupplierBillNumber()) ? dto.getSupplierBillNumber().trim() : null)
                .billDate(dto.getBillDate())
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .status(PurchaseStatus.CONFIRMED)
                .subtotalAmount(subtotal)
                .gstRatePercent(gstRate)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .paidAmount(initialPaid)
                .balanceAmount(balanceAmount)
                .build();

        purchaseEntry.recalculatePaymentStatus();

        PurchaseEntry savedPurchase = purchaseEntryRepository.save(purchaseEntry);

        List<PurchaseItemResponseDto> itemResponses = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO;

        for (int i = 0; i < dto.getItems().size(); i++) {
            PurchaseItemRequestDto itemDto = dto.getItems().get(i);
            Product product = products.get(i);

            BigDecimal roundedUnitPrice = itemDto.getUnitPrice().setScale(0, RoundingMode.HALF_UP);
            BigDecimal lineTotal = roundedUnitPrice.multiply(itemDto.getQuantity()).setScale(0, RoundingMode.HALF_UP);

            PurchaseItem purchaseItem = PurchaseItem.builder()
                    .purchaseEntry(savedPurchase)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .unitPrice(roundedUnitPrice)
                    .lineTotal(lineTotal)
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
                    .unitPrice(purchaseItem.getUnitPrice())
                    .lineTotal(purchaseItem.getLineTotal())
                    .currentStockBalance(newStockBalance)
                    .build());
        }

        // Record initial payment ledger entry if paid
        if (initialPaid.compareTo(BigDecimal.ZERO) > 0) {
            PurchasePayment payment = PurchasePayment.builder()
                    .purchaseEntry(savedPurchase)
                    .paymentDate(dto.getBillDate())
                    .amount(initialPaid)
                    .paymentMode(dto.getPaymentMode())
                    .referenceNumber(dto.getPaymentReference())
                    .remarks(StringUtils.hasText(dto.getPaymentRemarks())
                            ? dto.getPaymentRemarks().trim()
                            : "Initial payment against bill " + savedPurchase.getPurchaseNumber())
                    .createdBy(userId)
                    .build();
            purchasePaymentRepository.save(payment);
            savedPurchase.addPayment(payment);
        }

        purchaseEntryRepository.save(savedPurchase);
        log.info("Created purchase bill: {} (Subtotal: {}, 18% GST: {}, Total: {}, Paid: {}, Pending: {}) with {} items",
                savedPurchase.getPurchaseNumber(), subtotal, gstAmount, totalAmount, initialPaid, balanceAmount, itemResponses.size());

        return toResponseDto(savedPurchase, itemResponses, totalQty);
    }

    /**
     * Record a subsequent payment to supplier against a confirmed purchase bill.
     */
    @Transactional
    public PurchaseResponseDto recordPayment(UUID purchaseId, PurchasePaymentRequestDto dto, UUID userId) {
        PurchaseEntry purchase = purchaseEntryRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseEntry", "id", purchaseId));

        if (purchase.getStatus() != PurchaseStatus.CONFIRMED) {
            throw new BadRequestException("Payments can only be recorded against CONFIRMED purchase bills. Current status: " + purchase.getStatus());
        }

        // Auto-heal legacy purchase financial fields if total was 0
        if (purchase.getTotalAmount() == null || purchase.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            healPurchaseFinancials(purchase);
        }

        if (purchase.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Purchase bill " + purchase.getPurchaseNumber() + " is already fully settled.");
        }

        BigDecimal paymentAmount = dto.getAmount().setScale(0, RoundingMode.HALF_UP);
        if (paymentAmount.compareTo(purchase.getBalanceAmount()) > 0) {
            throw new BadRequestException(String.format(
                    "Payment amount %s exceeds current pending balance %s for bill %s",
                    paymentAmount, purchase.getBalanceAmount(), purchase.getPurchaseNumber()
            ));
        }

        PurchasePayment payment = PurchasePayment.builder()
                .purchaseEntry(purchase)
                .paymentDate(dto.getPaymentDate())
                .amount(paymentAmount)
                .paymentMode(dto.getPaymentMode())
                .referenceNumber(dto.getReferenceNumber())
                .remarks(dto.getRemarks())
                .createdBy(userId)
                .build();

        purchasePaymentRepository.save(payment);
        purchase.addPayment(payment);

        BigDecimal newPaid = purchase.getPaidAmount().add(paymentAmount);
        BigDecimal newBalance = purchase.getTotalAmount().subtract(newPaid);
        purchase.setPaidAmount(newPaid);
        purchase.setBalanceAmount(newBalance);
        purchase.recalculatePaymentStatus();

        purchaseEntryRepository.save(purchase);
        log.info("Recorded payment of {} against purchase bill {}. New paid: {}, balance: {}, status: {}",
                paymentAmount, purchase.getPurchaseNumber(), newPaid, newBalance, purchase.getPaymentStatus());

        return getPurchaseById(purchaseId);
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
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getLineTotal())
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
    @Transactional
    public PurchaseResponseDto getPurchaseById(UUID id) {
        PurchaseEntry purchase = purchaseEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseEntry", "id", id));

        // Auto-heal legacy purchase financial fields if total was 0
        if (purchase.getTotalAmount() == null || purchase.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            healPurchaseFinancials(purchase);
        }

        BigDecimal totalQty = BigDecimal.ZERO;
        List<PurchaseItemResponseDto> itemDtos = new ArrayList<>();
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        for (PurchaseItem item : purchase.getItems()) {
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            totalQty = totalQty.add(qty);

            Product product = item.getProduct();
            BigDecimal currentStock = product != null ? inventoryService.getCurrentStock(product.getId()) : BigDecimal.ZERO;

            // Resolve unit price (fallback to product default selling price if 0 or null for older records)
            BigDecimal unitPrice = item.getUnitPrice();
            if ((unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) == 0) && product != null && product.getDefaultSellingPrice() != null) {
                unitPrice = product.getDefaultSellingPrice().setScale(0, RoundingMode.HALF_UP);
            } else if (unitPrice == null) {
                unitPrice = BigDecimal.ZERO;
            }

            BigDecimal lineTotal = item.getLineTotal();
            if ((lineTotal == null || lineTotal.compareTo(BigDecimal.ZERO) == 0) && qty.compareTo(BigDecimal.ZERO) > 0) {
                lineTotal = unitPrice.multiply(qty).setScale(0, RoundingMode.HALF_UP);
            } else if (lineTotal == null) {
                lineTotal = BigDecimal.ZERO;
            }

            calculatedSubtotal = calculatedSubtotal.add(lineTotal);

            itemDtos.add(PurchaseItemResponseDto.builder()
                    .id(item.getId())
                    .productId(product != null ? product.getId() : null)
                    .itemCode(product != null ? product.getItemCode() : "—")
                    .flavourName(product != null ? product.getFlavourName() : "Flavour")
                    .bottleSizeLitre(product != null ? product.getBottleSizeLitre() : BigDecimal.valueOf(5.00))
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .currentStockBalance(currentStock)
                    .build());
        }

        return toResponseDto(purchase, itemDtos, totalQty, calculatedSubtotal);
    }

    /**
     * Auto-heal legacy purchase bills created prior to migration V18 where prices and totals were 0.
     */
    private void healPurchaseFinancials(PurchaseEntry purchase) {
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        if (purchase.getItems() != null) {
            for (PurchaseItem item : purchase.getItems()) {
                BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                Product product = item.getProduct();

                BigDecimal unitPrice = item.getUnitPrice();
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) == 0) {
                    if (product != null && product.getDefaultSellingPrice() != null) {
                        unitPrice = product.getDefaultSellingPrice().setScale(0, RoundingMode.HALF_UP);
                    } else {
                        unitPrice = BigDecimal.valueOf(540);
                    }
                    item.setUnitPrice(unitPrice);
                }

                BigDecimal lineTotal = item.getLineTotal();
                if ((lineTotal == null || lineTotal.compareTo(BigDecimal.ZERO) == 0) && qty.compareTo(BigDecimal.ZERO) > 0) {
                    lineTotal = unitPrice.multiply(qty).setScale(0, RoundingMode.HALF_UP);
                    item.setLineTotal(lineTotal);
                } else if (lineTotal == null) {
                    lineTotal = BigDecimal.ZERO;
                }

                calculatedSubtotal = calculatedSubtotal.add(lineTotal);
            }
        }

        BigDecimal gstRate = purchase.getGstRatePercent() != null ? purchase.getGstRatePercent() : BigDecimal.valueOf(18.00);
        BigDecimal gstAmount = calculatedSubtotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal totalAmount = calculatedSubtotal.add(gstAmount);
        BigDecimal paidAmount = purchase.getPaidAmount() != null ? purchase.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balanceAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        purchase.setSubtotalAmount(calculatedSubtotal);
        purchase.setGstRatePercent(gstRate);
        purchase.setGstAmount(gstAmount);
        purchase.setTotalAmount(totalAmount);
        purchase.setPaidAmount(paidAmount);
        purchase.setBalanceAmount(balanceAmount);
        purchase.recalculatePaymentStatus();

        purchaseEntryRepository.save(purchase);
        log.info("Auto-healed legacy purchase bill {}: subtotal={}, 18% GST={}, total={}, paid={}, balance={}",
                purchase.getPurchaseNumber(), calculatedSubtotal, gstAmount, totalAmount, paidAmount, balanceAmount);
    }

    /**
     * Search paginated purchases using dynamic JPA Specification.
     */
    @Transactional(readOnly = true)
    public PageResponse<PurchaseSummaryDto> getAllPurchases(
            UUID supplierId,
            PurchaseStatus status,
            PurchasePaymentStatus paymentStatus,
            String purchaseNumber,
            String supplierBillNumber,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        Specification<PurchaseEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (StringUtils.hasText(purchaseNumber)) {
                predicates.add(cb.like(cb.lower(root.get("purchaseNumber")), "%" + purchaseNumber.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(supplierBillNumber)) {
                predicates.add(cb.like(cb.lower(root.get("supplierBillNumber")), "%" + supplierBillNumber.trim().toLowerCase() + "%"));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("billDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("billDate"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PurchaseEntry> page = purchaseEntryRepository.findAll(spec, pageable);
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
        return toResponseDto(p, items, totalQty, null);
    }

    private PurchaseResponseDto toResponseDto(PurchaseEntry p, List<PurchaseItemResponseDto> items, BigDecimal totalQty, BigDecimal calculatedSubtotal) {
        List<PurchasePaymentResponseDto> paymentDtos = p.getPayments() != null
                ? p.getPayments().stream()
                .map(this::toPaymentResponseDto)
                .collect(Collectors.toList())
                : new ArrayList<>();

        // Resolve financial numbers safely (auto-fallback for legacy bills prior to migration V18)
        BigDecimal subtotal = (p.getSubtotalAmount() != null && p.getSubtotalAmount().compareTo(BigDecimal.ZERO) > 0)
                ? p.getSubtotalAmount()
                : (calculatedSubtotal != null ? calculatedSubtotal : BigDecimal.ZERO);

        BigDecimal gstRate = p.getGstRatePercent() != null ? p.getGstRatePercent() : BigDecimal.valueOf(18.00);

        BigDecimal gstAmount = (p.getGstAmount() != null && p.getGstAmount().compareTo(BigDecimal.ZERO) > 0)
                ? p.getGstAmount()
                : subtotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BigDecimal totalAmount = (p.getTotalAmount() != null && p.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                ? p.getTotalAmount()
                : subtotal.add(gstAmount);

        BigDecimal paidAmount = p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO;

        BigDecimal balanceAmount = (p.getBalanceAmount() != null && p.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                ? p.getBalanceAmount()
                : totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        PurchasePaymentStatus paymentStatus = p.getPaymentStatus();
        if (paymentStatus == null || paymentStatus == PurchasePaymentStatus.UNPAID) {
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0 && balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
                paymentStatus = PurchasePaymentStatus.PAID;
            } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentStatus = PurchasePaymentStatus.PARTIALLY_PAID;
            } else {
                paymentStatus = PurchasePaymentStatus.UNPAID;
            }
        }

        UUID supplierId = p.getSupplier() != null ? p.getSupplier().getId() : null;
        String supplierCode = p.getSupplier() != null ? p.getSupplier().getSupplierCode() : null;
        String supplierName = p.getSupplier() != null ? p.getSupplier().getSupplierName() : "Supplier";

        return PurchaseResponseDto.builder()
                .id(p.getId())
                .purchaseNumber(p.getPurchaseNumber())
                .supplierId(supplierId)
                .supplierCode(supplierCode)
                .supplierName(supplierName)
                .supplierBillNumber(p.getSupplierBillNumber())
                .billDate(p.getBillDate())
                .description(p.getDescription())
                .status(p.getStatus())
                .subtotalAmount(subtotal)
                .gstRatePercent(gstRate)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .balanceAmount(balanceAmount)
                .paymentStatus(paymentStatus)
                .cancelledReason(p.getCancelledReason())
                .cancelledAt(p.getCancelledAt())
                .cancelledBy(p.getCancelledBy())
                .items(items)
                .payments(paymentDtos)
                .itemCount(items != null ? items.size() : 0)
                .totalItems(items != null ? items.size() : 0)
                .totalQuantity(totalQty)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .createdBy(p.getCreatedBy())
                .updatedBy(p.getUpdatedBy())
                .build();
    }

    private PurchasePaymentResponseDto toPaymentResponseDto(PurchasePayment pay) {
        return PurchasePaymentResponseDto.builder()
                .id(pay.getId())
                .purchaseEntryId(pay.getPurchaseEntry() != null ? pay.getPurchaseEntry().getId() : null)
                .paymentDate(pay.getPaymentDate())
                .amount(pay.getAmount())
                .paymentMode(pay.getPaymentMode())
                .referenceNumber(pay.getReferenceNumber())
                .remarks(pay.getRemarks())
                .createdAt(pay.getCreatedAt())
                .createdBy(pay.getCreatedBy())
                .build();
    }

    private PurchaseSummaryDto toSummaryDto(PurchaseEntry p) {
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        if (p.getItems() != null) {
            for (PurchaseItem item : p.getItems()) {
                BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                totalQty = totalQty.add(qty);

                BigDecimal unitPrice = item.getUnitPrice();
                if ((unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) == 0) && item.getProduct() != null && item.getProduct().getDefaultSellingPrice() != null) {
                    unitPrice = item.getProduct().getDefaultSellingPrice().setScale(0, RoundingMode.HALF_UP);
                } else if (unitPrice == null) {
                    unitPrice = BigDecimal.ZERO;
                }

                BigDecimal lineTotal = item.getLineTotal();
                if ((lineTotal == null || lineTotal.compareTo(BigDecimal.ZERO) == 0) && qty.compareTo(BigDecimal.ZERO) > 0) {
                    lineTotal = unitPrice.multiply(qty).setScale(0, RoundingMode.HALF_UP);
                } else if (lineTotal == null) {
                    lineTotal = BigDecimal.ZERO;
                }

                calculatedSubtotal = calculatedSubtotal.add(lineTotal);
            }
        }

        BigDecimal subtotal = (p.getSubtotalAmount() != null && p.getSubtotalAmount().compareTo(BigDecimal.ZERO) > 0)
                ? p.getSubtotalAmount()
                : calculatedSubtotal;

        BigDecimal gstRate = p.getGstRatePercent() != null ? p.getGstRatePercent() : BigDecimal.valueOf(18.00);

        BigDecimal gstAmount = (p.getGstAmount() != null && p.getGstAmount().compareTo(BigDecimal.ZERO) > 0)
                ? p.getGstAmount()
                : subtotal.multiply(gstRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BigDecimal totalAmount = (p.getTotalAmount() != null && p.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                ? p.getTotalAmount()
                : subtotal.add(gstAmount);

        BigDecimal paidAmount = p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO;

        BigDecimal balanceAmount = (p.getBalanceAmount() != null && p.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                ? p.getBalanceAmount()
                : totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        PurchasePaymentStatus paymentStatus = p.getPaymentStatus();
        if (paymentStatus == null || paymentStatus == PurchasePaymentStatus.UNPAID) {
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0 && balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
                paymentStatus = PurchasePaymentStatus.PAID;
            } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentStatus = PurchasePaymentStatus.PARTIALLY_PAID;
            } else {
                paymentStatus = PurchasePaymentStatus.UNPAID;
            }
        }

        UUID supplierId = p.getSupplier() != null ? p.getSupplier().getId() : null;
        String supplierCode = p.getSupplier() != null ? p.getSupplier().getSupplierCode() : null;
        String supplierName = p.getSupplier() != null ? p.getSupplier().getSupplierName() : "Supplier";

        return PurchaseSummaryDto.builder()
                .id(p.getId())
                .purchaseNumber(p.getPurchaseNumber())
                .supplierId(supplierId)
                .supplierCode(supplierCode)
                .supplierName(supplierName)
                .supplierBillNumber(p.getSupplierBillNumber())
                .billDate(p.getBillDate())
                .status(p.getStatus())
                .subtotalAmount(subtotal)
                .gstRatePercent(gstRate)
                .gstAmount(gstAmount)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .balanceAmount(balanceAmount)
                .paymentStatus(paymentStatus)
                .itemCount(p.getItems() != null ? p.getItems().size() : 0)
                .totalItems(p.getItems() != null ? p.getItems().size() : 0)
                .totalQuantity(totalQty)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
