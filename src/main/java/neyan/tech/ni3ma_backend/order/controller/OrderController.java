package neyan.tech.ni3ma_backend.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.common.security.CurrentUser;
import neyan.tech.ni3ma_backend.common.security.UserPrincipal;
import neyan.tech.ni3ma_backend.order.dto.CreateOrderRequest;
import neyan.tech.ni3ma_backend.order.dto.OrderResponse;
import neyan.tech.ni3ma_backend.order.dto.OrderSummaryResponse;
import neyan.tech.ni3ma_backend.order.entity.OrderStatus;
import neyan.tech.ni3ma_backend.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Customer order endpoints")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create order", description = "Creates a new order (reservation)")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @CurrentUser UserPrincipal currentUser) {
        OrderResponse response = orderService.createOrder(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get my orders", description = "Returns paginated list of user's orders")
    @GetMapping("/my")
    public ResponseEntity<Page<OrderSummaryResponse>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        if (status != null) {
            return ResponseEntity.ok(orderService.getMyOrdersByStatus(currentUser.getId(), status, pageable));
        }
        return ResponseEntity.ok(orderService.getMyOrders(currentUser.getId(), pageable));
    }

    @Operation(summary = "Get order by ID", description = "Returns order details")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(orderService.getOrderById(id, currentUser.getId()));
    }

    @Operation(summary = "Cancel order", description = "Cancels an order before pickup window")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        OrderResponse response = orderService.cancelOrder(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}

