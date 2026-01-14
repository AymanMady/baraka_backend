package neyan.tech.ni3ma_backend.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.order.dto.OrderResponse;
import neyan.tech.ni3ma_backend.order.entity.Order;
import neyan.tech.ni3ma_backend.order.mapper.OrderMapper;
import neyan.tech.ni3ma_backend.order.repository.OrderRepository;
import neyan.tech.ni3ma_backend.order.service.OrderService;
import neyan.tech.ni3ma_backend.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Orders", description = "Admin order management endpoints")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Operation(summary = "Get all orders", description = "Returns paginated list of all orders")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<Order> orders = orderRepository.findAll(pageable);

        return ResponseEntity.ok(orders.map(orderMapper::toResponse));
    }

    @Operation(summary = "Get order by ID", description = "Returns order details")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id));
        return ResponseEntity.ok(orderMapper.toResponse(order));
    }
}
