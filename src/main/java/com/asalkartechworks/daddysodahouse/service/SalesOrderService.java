package com.asalkartechworks.daddysodahouse.service;

import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.order.OrderCancelRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderItemRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderItemResponseDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderResponseDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.Franchise;
import com.asalkartechworks.daddysodahouse.entity.Product;
import com.asalkartechworks.daddysodahouse.entity.SalesOrder;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderItem;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.entity.StockMovementType;
import com.asalkartechworks.daddysodahouse.entity.UserRole;
import com.asalkartechworks.daddysodahouse.exception.BadRequestException;
import com.asalkartechworks.daddysodahouse.exception.ResourceNotFoundException;
import com.asalkartechworks.daddysodahouse.repository.FranchiseRepository;
import com.asalkartechworks.daddysodahouse.repository.ProductRepository;
import com.asalkartechworks.daddysodahouse.repository.SalesOrderRepository;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final FranchiseRepository franchiseRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    /**
     * Create a new sales order in DRAFT or PLACED status.
     * Warehouse stock is NOT deducted at order creation time.
     */
    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDto dto, UserPrincipal principal) {
        boolean isFranchiseOwner = principal.getRole() == UserRole.FRANCHISE_OWNER;

        Franchise franchise;
        if (isFranchiseOwner) {
            UUID userFranchiseId = principal.getFranchiseId();
            if (userFranchiseId == null) {
                throw new BadRequestException("No franchise mapped to the authenticated franchise owner");
            }
            if (dto.getFranchiseId() != null && !dto.getFranchiseId().equals(userFranchiseId)) {
                throw new BadRequestException("Franchise owner cannot create orders for another franchise");
            }
            franchise = franchiseRepository.findById(userFranchiseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", userFranchiseId));
        } else {
            if (dto.getFranchiseId() == null) {
                throw new BadRequestException("Franchise ID is mandatory for manager/super admin order creation");
            }
            franchise = franchiseRepository.findById(dto.getFranchiseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", dto.getFranchiseId()));
        }

        if (!franchise.isActive()) {
            throw new BadRequestException("Cannot create order for an inactive franchise: " + franchise.getBusinessName());
        }

        LocalDate orderDate = dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now();
        if (orderDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Order date cannot be in the future: " + orderDate);
        }

        SalesOrderStatus status = dto.getStatus() != null ? dto.getStatus() : SalesOrderStatus.PLACED;
        if (status != SalesOrderStatus.DRAFT && status != SalesOrderStatus.PLACED) {
            throw new BadRequestException("Initial order status can only be DRAFT or PLACED. Supplied: " + status);
        }

        String orderNumber = generateOrderNumber();

        SalesOrder order = SalesOrder.builder()
                .orderNumber(orderNumber)
                .franchise(franchise)
                .orderDate(orderDate)
                .status(status)
                .notes(StringUtils.hasText(dto.getNotes()) ? dto.getNotes().trim() : null)
                .build();

        SalesOrder savedOrder = salesOrderRepository.save(order);

        List<SalesOrderItem> items = buildAndValidateOrderItems(savedOrder, dto.getItems(), isFranchiseOwner);
        for (SalesOrderItem item : items) {
            savedOrder.addItem(item);
        }

        salesOrderRepository.save(savedOrder);
        log.info("Created sales order: {} with {} items and status: {}", savedOrder.getOrderNumber(), items.size(), savedOrder.getStatus());

        return toResponseDto(savedOrder);
    }

    /**
     * Get single order by ID with role-based isolation.
     */
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(UUID id, UserPrincipal principal) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        verifyFranchiseOwnershipIfOwner(order, principal);
        return toResponseDto(order);
    }

    /**
     * Get paginated orders list for Super Admin and Manager.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> getAllOrders(
            UUID franchiseId,
            SalesOrderStatus status,
            String orderNumber,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        Page<SalesOrder> page = salesOrderRepository.searchOrders(
                franchiseId, status, orderNumber, fromDate, toDate, pageable);

        return PageResponse.from(page.map(this::toSummaryDto));
    }

    /**
     * Get paginated orders list strictly for the logged-in Franchise Owner.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> getMyOrders(
            UserPrincipal principal,
            SalesOrderStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        UUID franchiseId = principal.getFranchiseId();
        if (franchiseId == null) {
            throw new ResourceNotFoundException("Franchise mapping", "user", principal.getId());
        }

        Page<SalesOrder> page = salesOrderRepository.searchOrders(
                franchiseId, status, null, fromDate, toDate, pageable);

        return PageResponse.from(page.map(this::toSummaryDto));
    }

    /**
     * Update order details (only in DRAFT or PLACED status).
     * Franchise owners can only update their own DRAFT orders.
     */
    @Transactional
    public OrderResponseDto updateOrder(UUID id, OrderUpdateRequestDto dto, UserPrincipal principal) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        boolean isFranchiseOwner = principal.getRole() == UserRole.FRANCHISE_OWNER;
        verifyFranchiseOwnershipIfOwner(order, principal);

        if (order.getStatus() == SalesOrderStatus.DELIVERED || order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot edit order in " + order.getStatus() + " status. Delivered and cancelled orders are immutable.");
        }

        if (isFranchiseOwner && order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new BadRequestException("Franchise owners can only edit orders in DRAFT status. Current status: " + order.getStatus());
        }

        if (isFranchiseOwner && dto.getFranchiseId() != null && !order.getFranchise().getId().equals(dto.getFranchiseId())) {
            throw new BadRequestException("Franchise owners cannot reassign order to another franchise");
        }

        if (!isFranchiseOwner && dto.getFranchiseId() != null && !order.getFranchise().getId().equals(dto.getFranchiseId())) {
            Franchise newFranchise = franchiseRepository.findById(dto.getFranchiseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Franchise", "id", dto.getFranchiseId()));
            if (!newFranchise.isActive()) {
                throw new BadRequestException("Cannot reassign order to an inactive franchise: " + newFranchise.getBusinessName());
            }
            order.setFranchise(newFranchise);
        }

        if (dto.getOrderDate() != null) {
            order.setOrderDate(dto.getOrderDate());
        }

        order.setNotes(StringUtils.hasText(dto.getNotes()) ? dto.getNotes().trim() : null);

        String reasonText = StringUtils.hasText(dto.getReason()) ? dto.getReason().trim()
                : StringUtils.hasText(dto.getUpdateReason()) ? dto.getUpdateReason().trim() : null;
        if (StringUtils.hasText(reasonText)) {
            order.setUpdateReason(reasonText);
        }

        // Update items in-place to prevent unique constraint collisions
        updateOrderItems(order, dto.getItems(), isFranchiseOwner);

        SalesOrder saved = salesOrderRepository.save(order);
        log.info("Updated sales order ID: {}", saved.getId());
        return toResponseDto(saved);
    }

    /**
     * Move DRAFT order to PLACED status.
     */
    @Transactional
    public OrderResponseDto placeOrder(UUID id, UserPrincipal principal) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        verifyFranchiseOwnershipIfOwner(order, principal);

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT orders can be moved to PLACED status. Current status: " + order.getStatus());
        }

        if (order.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place an order with no items");
        }

        order.setStatus(SalesOrderStatus.PLACED);
        SalesOrder saved = salesOrderRepository.save(order);
        log.info("Order {} moved from DRAFT to PLACED", saved.getOrderNumber());
        return toResponseDto(saved);
    }

    /**
     * Move PLACED order to PREPARED status (SUPER_ADMIN and MANAGER only).
     */
    @Transactional
    public OrderResponseDto prepareOrder(UUID id, UserPrincipal principal) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        if (order.getStatus() != SalesOrderStatus.PLACED) {
            throw new BadRequestException("Only PLACED orders can be moved to PREPARED status. Current status: " + order.getStatus());
        }

        order.setStatus(SalesOrderStatus.PREPARED);
        order.setPreparedAt(Instant.now());
        order.setPreparedBy(principal.getId());

        SalesOrder saved = salesOrderRepository.save(order);
        log.info("Order {} marked as PREPARED", saved.getOrderNumber());
        return toResponseDto(saved);
    }

    /**
     * Mark order as DELIVERED and deduct warehouse inventory stock with pessimistic locking.
     * Full delivery only; rejects and rolls back if any product has insufficient stock.
     */
    @Transactional
    public OrderResponseDto deliverOrder(UUID id, UserPrincipal principal) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        if (order.getStatus() != SalesOrderStatus.PLACED && order.getStatus() != SalesOrderStatus.PREPARED) {
            throw new BadRequestException("Only PLACED or PREPARED orders can be marked DELIVERED. Current status: " + order.getStatus());
        }

        // Pre-flight check: ensure every ordered product has sufficient warehouse stock
        List<String> shortages = new ArrayList<>();
        for (SalesOrderItem item : order.getItems()) {
            BigDecimal currentStock = inventoryService.getCurrentStock(item.getProduct().getId());
            if (currentStock.compareTo(item.getQuantity()) < 0) {
                shortages.add(String.format("%s (%s): Available %s, Required %s",
                        item.getProduct().getFlavourName(), item.getProduct().getItemCode(), currentStock, item.getQuantity()));
            }
        }

        if (!shortages.isEmpty()) {
            throw new BadRequestException("Insufficient warehouse stock for delivery: " + String.join("; ", shortages));
        }

        // Deduct stock for each item using pessimistic lock and write DELIVERY_OUT movement
        for (SalesOrderItem item : order.getItems()) {
            inventoryService.decreaseStock(
                    item.getProduct(),
                    item.getQuantity(),
                    StockMovementType.DELIVERY_OUT,
                    "SALES_ORDER",
                    order.getId(),
                    order.getOrderNumber(),
                    "Delivered sales order: " + order.getOrderNumber() + " to " + order.getFranchise().getBusinessName(),
                    principal.getId()
            );
        }

        order.setStatus(SalesOrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        order.setDeliveredBy(principal.getId());

        SalesOrder saved = salesOrderRepository.save(order);
        log.info("Order {} successfully DELIVERED and warehouse stock deducted", saved.getOrderNumber());

        return toResponseDto(saved);
    }

    /**
     * Cancel an order (DRAFT or PLACED).
     */
    @Transactional
    public OrderResponseDto cancelOrder(UUID id, OrderCancelRequestDto dto, UserPrincipal principal) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        boolean isFranchiseOwner = principal.getRole() == UserRole.FRANCHISE_OWNER;
        verifyFranchiseOwnershipIfOwner(order, principal);

        if (order.getStatus() == SalesOrderStatus.DELIVERED) {
            throw new BadRequestException("DELIVERED orders cannot be cancelled");
        }

        if (order.getStatus() == SalesOrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already CANCELLED");
        }

        if (isFranchiseOwner && order.getStatus() != SalesOrderStatus.DRAFT && order.getStatus() != SalesOrderStatus.PLACED) {
            throw new BadRequestException("Franchise owners can only cancel orders in DRAFT or PLACED status. Current status: " + order.getStatus());
        }

        order.setStatus(SalesOrderStatus.CANCELLED);
        order.setCancellationReason(dto.getReason().trim());
        order.setCancelledAt(Instant.now());
        order.setCancelledBy(principal.getId());

        SalesOrder saved = salesOrderRepository.save(order);
        log.info("Order {} CANCELLED with reason: {}", saved.getOrderNumber(), dto.getReason());
        return toResponseDto(saved);
    }

    /**
     * Physically delete only DRAFT orders.
     */
    @Transactional
    public void deleteDraftOrder(UUID id, UserPrincipal principal) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        verifyFranchiseOwnershipIfOwner(order, principal);

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT orders can be deleted. Current status: " + order.getStatus());
        }

        salesOrderRepository.delete(order);
        log.info("Deleted DRAFT sales order: {}", order.getOrderNumber());
    }

    private List<SalesOrderItem> buildAndValidateOrderItems(SalesOrder order, List<OrderItemRequestDto> itemDtos, boolean isFranchiseOwner) {
        if (itemDtos == null || itemDtos.isEmpty()) {
            throw new BadRequestException("Order items list cannot be empty");
        }

        Set<UUID> seenProductIds = new HashSet<>();
        List<SalesOrderItem> items = new ArrayList<>();

        for (OrderItemRequestDto itemDto : itemDtos) {
            if (!seenProductIds.add(itemDto.getProductId())) {
                throw new BadRequestException("Duplicate product in order: " + itemDto.getProductId());
            }

            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemDto.getProductId()));

            if (!product.isActive()) {
                throw new BadRequestException("Cannot add inactive product to order: " + product.getFlavourName());
            }

            BigDecimal unitPrice;
            if (isFranchiseOwner) {
                if (itemDto.getUnitPrice() != null) {
                    throw new BadRequestException("Franchise owners are not permitted to set custom unit prices");
                }
                // Franchise owner strictly uses current product default selling price
                if (product.getDefaultSellingPrice() == null) {
                    throw new BadRequestException("Product " + product.getFlavourName() + " has no default selling price configured");
                }
                unitPrice = product.getDefaultSellingPrice();
            } else {
                // Manager/Admin can override price or fall back to product default selling price
                if (itemDto.getUnitPrice() != null) {
                    unitPrice = itemDto.getUnitPrice();
                } else if (product.getDefaultSellingPrice() != null) {
                    unitPrice = product.getDefaultSellingPrice();
                } else {
                    throw new BadRequestException("No unit price supplied and product " + product.getFlavourName() + " has no default selling price configured");
                }
            }

            BigDecimal lineTotal = itemDto.getQuantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            SalesOrderItem item = SalesOrderItem.builder()
                    .salesOrder(order)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .originalQuantity(itemDto.getOriginalQuantity() != null ? itemDto.getOriginalQuantity() : itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .adminNote(StringUtils.hasText(itemDto.getAdminNote()) ? itemDto.getAdminNote().trim() : null)
                    .build();

            items.add(item);
        }

        return items;
    }

    private void updateOrderItems(SalesOrder order, List<OrderItemRequestDto> itemDtos, boolean isFranchiseOwner) {
        if (itemDtos == null || itemDtos.isEmpty()) {
            throw new BadRequestException("Order items list cannot be empty");
        }

        Set<UUID> seenProductIds = new HashSet<>();
        Map<UUID, SalesOrderItem> existingItemsMap = new HashMap<>();
        for (SalesOrderItem existingItem : order.getItems()) {
            if (existingItem.getProduct() != null) {
                existingItemsMap.put(existingItem.getProduct().getId(), existingItem);
            }
        }

        List<SalesOrderItem> updatedList = new ArrayList<>();

        for (OrderItemRequestDto itemDto : itemDtos) {
            UUID productId = itemDto.getProductId();
            if (!seenProductIds.add(productId)) {
                throw new BadRequestException("Duplicate product in order: " + productId);
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

            if (!product.isActive()) {
                throw new BadRequestException("Cannot add inactive product to order: " + product.getFlavourName());
            }

            BigDecimal unitPrice;
            if (isFranchiseOwner) {
                if (itemDto.getUnitPrice() != null) {
                    throw new BadRequestException("Franchise owners are not permitted to set custom unit prices");
                }
                if (product.getDefaultSellingPrice() == null) {
                    throw new BadRequestException("Product " + product.getFlavourName() + " has no default selling price configured");
                }
                unitPrice = product.getDefaultSellingPrice();
            } else {
                if (itemDto.getUnitPrice() != null) {
                    unitPrice = itemDto.getUnitPrice();
                } else if (product.getDefaultSellingPrice() != null) {
                    unitPrice = product.getDefaultSellingPrice();
                } else {
                    throw new BadRequestException("No unit price supplied and product " + product.getFlavourName() + " has no default selling price configured");
                }
            }

            BigDecimal lineTotal = itemDto.getQuantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            SalesOrderItem existingItem = existingItemsMap.get(productId);
            if (existingItem != null) {
                if (existingItem.getOriginalQuantity() == null) {
                    existingItem.setOriginalQuantity(existingItem.getQuantity());
                }
                existingItem.setQuantity(itemDto.getQuantity());
                existingItem.setUnitPrice(unitPrice);
                existingItem.setLineTotal(lineTotal);
                if (StringUtils.hasText(itemDto.getAdminNote())) {
                    existingItem.setAdminNote(itemDto.getAdminNote().trim());
                }
                updatedList.add(existingItem);
            } else {
                BigDecimal origQty = itemDto.getOriginalQuantity() != null ? itemDto.getOriginalQuantity() : itemDto.getQuantity();
                SalesOrderItem newItem = SalesOrderItem.builder()
                        .salesOrder(order)
                        .product(product)
                        .quantity(itemDto.getQuantity())
                        .originalQuantity(origQty)
                        .unitPrice(unitPrice)
                        .lineTotal(lineTotal)
                        .adminNote(StringUtils.hasText(itemDto.getAdminNote()) ? itemDto.getAdminNote().trim() : null)
                        .build();
                updatedList.add(newItem);
            }
        }

        // Remove items that are no longer present in the updated payload
        order.getItems().removeIf(item -> item.getProduct() == null || !seenProductIds.contains(item.getProduct().getId()));

        // Add any new items to the collection
        for (SalesOrderItem item : updatedList) {
            if (!order.getItems().contains(item)) {
                order.addItem(item);
            }
        }
    }

    private void verifyFranchiseOwnershipIfOwner(SalesOrder order, UserPrincipal principal) {
        if (principal.getRole() == UserRole.FRANCHISE_OWNER) {
            UUID userFranchiseId = principal.getFranchiseId();
            if (userFranchiseId == null || !order.getFranchise().getId().equals(userFranchiseId)) {
                throw new AccessDeniedException("Access denied: You do not have permission to view or manage orders belonging to another franchise");
            }
        }
    }

    private synchronized String generateOrderNumber() {
        LocalDate now = LocalDate.now();
        String prefix = String.format("ORD-%d%02d-", now.getYear(), now.getMonthValue());

        return salesOrderRepository.findMaxOrderNumberWithPrefix(prefix)
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

    private OrderResponseDto toResponseDto(SalesOrder order) {
        List<OrderItemResponseDto> itemDtos = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal orderTotal = BigDecimal.ZERO;

        for (SalesOrderItem item : order.getItems()) {
            totalQuantity = totalQuantity.add(item.getQuantity());
            if (item.getLineTotal() != null) {
                orderTotal = orderTotal.add(item.getLineTotal());
            }

            itemDtos.add(OrderItemResponseDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .itemCode(item.getProduct().getItemCode())
                    .flavourName(item.getProduct().getFlavourName())
                    .bottleSizeLitre(item.getProduct().getBottleSizeLitre())
                    .quantity(item.getQuantity())
                    .originalQuantity(item.getOriginalQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getLineTotal())
                    .adminNote(item.getAdminNote())
                    .build());
        }

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .franchiseId(order.getFranchise().getId())
                .franchiseCode(order.getFranchise().getFranchiseCode())
                .franchiseBusinessName(order.getFranchise().getBusinessName())
                .franchiseOwnerName(order.getFranchise().getOwnerName())
                .franchiseMobile(order.getFranchise().getMobile())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .notes(order.getNotes())
                .updateReason(order.getUpdateReason())
                .cancellationReason(order.getCancellationReason())
                .cancelledAt(order.getCancelledAt())
                .cancelledBy(order.getCancelledBy())
                .preparedAt(order.getPreparedAt())
                .preparedBy(order.getPreparedBy())
                .deliveredAt(order.getDeliveredAt())
                .deliveredBy(order.getDeliveredBy())
                .items(itemDtos)
                .totalQuantity(totalQuantity)
                .orderTotal(orderTotal)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .createdBy(order.getCreatedBy())
                .updatedBy(order.getUpdatedBy())
                .build();
    }

    private OrderSummaryDto toSummaryDto(SalesOrder order) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal orderTotal = BigDecimal.ZERO;

        for (SalesOrderItem item : order.getItems()) {
            totalQuantity = totalQuantity.add(item.getQuantity());
            if (item.getLineTotal() != null) {
                orderTotal = orderTotal.add(item.getLineTotal());
            }
        }

        return OrderSummaryDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .franchiseId(order.getFranchise().getId())
                .franchiseCode(order.getFranchise().getFranchiseCode())
                .franchiseBusinessName(order.getFranchise().getBusinessName())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .updateReason(order.getUpdateReason())
                .itemCount(order.getItems().size())
                .totalQuantity(totalQuantity)
                .orderTotal(orderTotal)
                .createdBy(order.getCreatedBy())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
