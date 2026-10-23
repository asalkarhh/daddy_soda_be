package com.asalkartechworks.daddysodahouse.controller;

import com.asalkartechworks.daddysodahouse.dto.ApiResponse;
import com.asalkartechworks.daddysodahouse.dto.common.PageResponse;
import com.asalkartechworks.daddysodahouse.dto.order.OrderCancelRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderCreateRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderResponseDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderDispatchRequestDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderSummaryDto;
import com.asalkartechworks.daddysodahouse.dto.order.OrderUpdateRequestDto;
import com.asalkartechworks.daddysodahouse.entity.SalesOrderStatus;
import com.asalkartechworks.daddysodahouse.security.UserPrincipal;
import com.asalkartechworks.daddysodahouse.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for Franchise Sales Orders, Delivery, and Warehouse Stock Deduction (BE-05).
 *
 * Order Status Lifecycle:
 *   DRAFT -> PLACED -> PREPARED -> DELIVERED
 *     \        \
 *      -> CANCELLED
 *
 * Rules:
 * - FRANCHISE_OWNER can place orders only for their own franchise with auto-filled default selling prices.
 * - SUPER_ADMIN and MANAGER can create orders for any franchise and optionally override unit prices.
 * - Stock deduction occurs strictly and atomically at DELIVERED time (no reservation at order placement).
 * - Full delivery only; insufficient stock on any item prevents delivery and rolls back completely.
 * - Confirmed DELIVERED orders cannot be edited, cancelled, or deleted.
 * - Only DRAFT orders can be deleted.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Sales Orders", description = "Franchise sales orders, lifecycle transitions, delivery, and stock deduction")
@SecurityRequirement(name = "Bearer Authentication")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Create sales order",
            description = """
                    Create a new sales order in DRAFT or PLACED status.
                    - For SUPER_ADMIN/MANAGER: franchiseId is mandatory; unitPrice is optional (defaults to Product Master price).
                    - For FRANCHISE_OWNER: franchiseId is auto-derived from authenticated user; manual unitPrice is forbidden.
                    - Stock is NOT deducted upon order creation.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Sales order creation payload",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = OrderCreateRequestDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Super Admin / Manager with custom price override",
                                            description = "Manager creates an order specifying franchiseId and overriding unit price",
                                            value = """
                                                    {
                                                      "franchiseId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                                                      "orderDate": "2026-09-23",
                                                      "status": "PLACED",
                                                      "notes": "Deliver along with next route",
                                                      "items": [
                                                        {
                                                          "productId": "11111111-1111-1111-1111-111111111111",
                                                          "quantity": 5.0,
                                                          "unitPrice": 450.00
                                                        },
                                                        {
                                                          "productId": "22222222-2222-2222-2222-222222222222",
                                                          "quantity": 3.0,
                                                          "unitPrice": 420.00
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Franchise Owner with auto-filled product prices",
                                            description = "Franchise owner orders for their own franchise using catalog default prices",
                                            value = """
                                                    {
                                                      "orderDate": "2026-09-23",
                                                      "status": "PLACED",
                                                      "notes": "Please deliver this week",
                                                      "items": [
                                                        {
                                                          "productId": "11111111-1111-1111-1111-111111111111",
                                                          "quantity": 5.0
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderCreateRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponseDto response = salesOrderService.createOrder(dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Sales order created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "List sales orders",
            description = "Get paginated order summaries with franchise, status, orderNumber, and date range filters (SUPER_ADMIN and MANAGER only)"
    )
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryDto>>> getAllOrders(
            @Parameter(description = "Filter by Franchise ID") @RequestParam(required = false) UUID franchiseId,
            @Parameter(description = "Filter by Order Status") @RequestParam(required = false) SalesOrderStatus status,
            @Parameter(description = "Filter by Order Number (exact match)") @RequestParam(required = false) String orderNumber,
            @Parameter(description = "Filter by order date from (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Filter by order date to (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<OrderSummaryDto> response = salesOrderService.getAllOrders(
                franchiseId, status, orderNumber, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales orders retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Get sales order by ID",
            description = "Retrieve complete sales order details including franchise info, items, price snapshots, and status history. Franchise owners can only view their own franchise orders."
    )
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponseDto response = salesOrderService.getOrderById(id, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Update sales order",
            description = "Update editable order details. Editable only in DRAFT or PLACED status (DELIVERED and CANCELLED cannot be edited). Franchise owners can update only their own DRAFT orders without overriding prices."
    )
    public ResponseEntity<ApiResponse<OrderResponseDto>> updateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody OrderUpdateRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponseDto response = salesOrderService.updateOrder(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order updated successfully"));
    }

    @PostMapping("/{id}/place")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Place draft sales order",
            description = "Transition order status from DRAFT to PLACED. Validates that the order has at least one item. Does not deduct inventory stock."
    )
    public ResponseEntity<ApiResponse<OrderResponseDto>> placeOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponseDto response = salesOrderService.placeOrder(id, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order placed successfully"));
    }

    @PostMapping("/{id}/prepare")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Mark sales order as prepared",
            description = "Transition order status from PLACED to PREPARED (SUPER_ADMIN and MANAGER only). Sets prepared timestamp and user. Does not deduct stock."
    )
    public ResponseEntity<ApiResponse<OrderResponseDto>> prepareOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponseDto response = salesOrderService.prepareOrder(id, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order marked as prepared"));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Deliver sales order and deduct warehouse stock",
            description = """
                    Transition order status from PLACED or PREPARED to DELIVERED (SUPER_ADMIN and MANAGER only).
                    - Validates warehouse inventory stock for each ordered flavour.
                    - If any flavour has insufficient stock, rejects delivery and deducts 0 stock (atomic rollback).
                    - If stock is sufficient, deducts inventory using pessimistic write locks and creates a DELIVERY_OUT movement per item.
                    - Delivered orders become strictly immutable.
                    """
    )
    public ResponseEntity<ApiResponse<OrderResponseDto>> deliverOrder(
            @PathVariable UUID id,
            @Valid @RequestBody OrderDispatchRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponseDto response = salesOrderService.deliverOrder(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order marked as delivered and warehouse stock deducted"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Cancel sales order",
            description = "Cancel an order in DRAFT or PLACED status. Cancellation reason is mandatory (max 500 chars). DELIVERED orders cannot be cancelled. No inventory stock is touched."
    )
    public ResponseEntity<ApiResponse<OrderResponseDto>> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody OrderCancelRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderResponseDto response = salesOrderService.cancelOrder(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales order cancelled successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'FRANCHISE_OWNER')")
    @Operation(
            summary = "Delete draft sales order",
            description = "Physically delete a sales order. Allowed ONLY for orders in DRAFT status. PLACED, PREPARED, DELIVERED, and CANCELLED orders cannot be deleted."
    )
    public ResponseEntity<ApiResponse<Void>> deleteDraftOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        salesOrderService.deleteDraftOrder(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "DRAFT sales order deleted successfully"));
    }
}
