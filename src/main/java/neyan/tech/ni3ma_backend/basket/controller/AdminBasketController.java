package neyan.tech.ni3ma_backend.basket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.basket.dto.BasketResponse;
import neyan.tech.ni3ma_backend.basket.entity.Basket;
import neyan.tech.ni3ma_backend.basket.entity.BasketStatus;
import neyan.tech.ni3ma_backend.basket.mapper.BasketMapper;
import neyan.tech.ni3ma_backend.basket.repository.BasketRepository;
import neyan.tech.ni3ma_backend.basket.service.BasketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/baskets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Baskets", description = "Admin basket management endpoints")
public class AdminBasketController {

    private final BasketService basketService;
    private final BasketRepository basketRepository;
    private final BasketMapper basketMapper;

    @Operation(summary = "Get all baskets", description = "Returns paginated list of all baskets (any status)")
    @GetMapping
    public ResponseEntity<Page<BasketResponse>> getAllBaskets(
            @RequestParam(required = false) BasketStatus status,
            @RequestParam(required = false) UUID shopId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Basket> baskets;
        if (status != null && shopId != null) {
            baskets = basketRepository.findByShopIdAndStatus(shopId, status, pageable);
        } else if (status != null) {
            baskets = basketRepository.findByStatus(status, pageable);
        } else if (shopId != null) {
            java.util.List<Basket> basketList = basketRepository.findByShopId(shopId);
            baskets = basketList.stream()
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toList(),
                            list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())));
        } else {
            baskets = basketRepository.findAll(pageable);
        }

        return ResponseEntity.ok(baskets.map(basketMapper::toResponse));
    }

    @Operation(summary = "Get basket by ID", description = "Returns basket details")
    @GetMapping("/{id}")
    public ResponseEntity<BasketResponse> getBasketById(@PathVariable UUID id) {
        return ResponseEntity.ok(basketService.getBasketById(id));
    }
}
