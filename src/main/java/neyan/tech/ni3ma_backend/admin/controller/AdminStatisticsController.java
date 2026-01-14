package neyan.tech.ni3ma_backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.admin.dto.StatisticsResponse;
import neyan.tech.ni3ma_backend.basket.repository.BasketRepository;
import neyan.tech.ni3ma_backend.order.repository.OrderRepository;
import neyan.tech.ni3ma_backend.shop.repository.ShopRepository;
import neyan.tech.ni3ma_backend.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Statistics", description = "Admin statistics endpoints")
public class AdminStatisticsController {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final BasketRepository basketRepository;
    private final OrderRepository orderRepository;

    @Operation(summary = "Get statistics", description = "Returns global statistics (users, shops, baskets, orders)")
    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics() {
        StatisticsResponse statistics = StatisticsResponse.builder()
                .totalUsers(userRepository.count())
                .totalShops(shopRepository.count())
                .totalBaskets(basketRepository.count())
                .totalOrders(orderRepository.count())
                .build();
        return ResponseEntity.ok(statistics);
    }
}
