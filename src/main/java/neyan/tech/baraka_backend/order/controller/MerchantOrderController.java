package neyan.tech.baraka_backend.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import neyan.tech.baraka_backend.common.security.CurrentUser;
import neyan.tech.baraka_backend.common.security.UserPrincipal;
import neyan.tech.baraka_backend.order.dto.OrderResponse;
import neyan.tech.baraka_backend.order.dto.OrderSummaryResponse;
import neyan.tech.baraka_backend.order.entity.OrderStatus;
import neyan.tech.baraka_backend.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
@Tag(name = "Merchant - Orders", description = "Merchant order management endpoints")
public class MerchantOrderController {

    private final OrderService orderService;

    @Operation(summary = "Validate pickup", description = "Validates customer pickup using pickup code")
    @PostMapping("/pickup")
    public ResponseEntity<OrderResponse> validatePickup(
            @RequestParam @NotBlank String pickupCode,
            @CurrentUser UserPrincipal currentUser) {
        OrderResponse response = orderService.validatePickup(pickupCode, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get order by pickup code", description = "Returns order details for validation")
    @GetMapping("/by-code")
    public ResponseEntity<OrderResponse> getOrderByPickupCode(
            @RequestParam @NotBlank String pickupCode) {
        return ResponseEntity.ok(orderService.getOrderByPickupCode(pickupCode));
    }

    @Operation(summary = "Get shop orders", description = "Returns paginated list of orders for a shop")
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<Page<OrderSummaryResponse>> getShopOrders(
            @PathVariable UUID shopId,
            @RequestParam(required = false) OrderStatus status,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getShopOrders(shopId, currentUser.getId(), pageable));
    }

    @Operation(summary = "Mark as no-show", description = "Marks an order as no-show after pickup window")
    @PostMapping("/{id}/no-show")
    public ResponseEntity<OrderResponse> markAsNoShow(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        OrderResponse response = orderService.markAsNoShow(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}

