package neyan.tech.ni3ma_backend.basket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.basket.dto.BasketResponse;
import neyan.tech.ni3ma_backend.basket.service.BasketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/baskets")
@RequiredArgsConstructor
@Tag(name = "Baskets", description = "Public basket endpoints")
public class BasketController {

    private final BasketService basketService;

    @Operation(summary = "Get available baskets", description = "Returns paginated list of published baskets")
    @GetMapping
    public ResponseEntity<Page<BasketResponse>> getBaskets(
            @RequestParam(required = false) UUID shopId,
            @PageableDefault(size = 20) Pageable pageable) {

        if (shopId != null) {
            return ResponseEntity.ok(basketService.getBasketsByShop(shopId, pageable));
        }
        return ResponseEntity.ok(basketService.getAvailableBaskets(pageable));
    }

    @Operation(summary = "Get basket by ID", description = "Returns basket details")
    @GetMapping("/{id}")
    public ResponseEntity<BasketResponse> getBasketById(@PathVariable UUID id) {
        return ResponseEntity.ok(basketService.getBasketById(id));
    }

    @Operation(summary = "Get available baskets for a shop", description = "Returns available baskets for pickup")
    @GetMapping("/shop/{shopId}/available")
    public ResponseEntity<List<BasketResponse>> getAvailableBasketsForShop(@PathVariable UUID shopId) {
        return ResponseEntity.ok(basketService.getAvailableBasketsForShop(shopId));
    }
}

