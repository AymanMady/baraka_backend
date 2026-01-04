package neyan.tech.baraka_backend.shop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.baraka_backend.shop.dto.ShopResponse;
import neyan.tech.baraka_backend.shop.entity.Shop;
import neyan.tech.baraka_backend.shop.mapper.ShopMapper;
import neyan.tech.baraka_backend.shop.repository.ShopRepository;
import neyan.tech.baraka_backend.shop.service.ShopService;
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
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Tag(name = "Shops", description = "Public shop endpoints")
public class ShopController {

    private final ShopService shopService;
    private final ShopRepository shopRepository;
    private final ShopMapper shopMapper;

    @Operation(summary = "Get active shops", description = "Returns paginated list of active shops")
    @GetMapping
    public ResponseEntity<Page<ShopResponse>> getShops(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "10") Double radiusKm,
            @PageableDefault(size = 20) Pageable pageable) {

        if (lat != null && lng != null) {
            // Search by location
            List<Shop> nearbyShops = shopRepository.findNearbyShops(lat, lng, radiusKm);
            List<ShopResponse> responses = shopMapper.toResponseList(nearbyShops);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(responses, pageable, responses.size()));
        } else if (city != null && !city.isBlank()) {
            // Search by city
            return ResponseEntity.ok(shopService.getShopsByCity(city, pageable));
        } else {
            // Return all active shops
            return ResponseEntity.ok(shopService.getActiveShops(pageable));
        }
    }

    @Operation(summary = "Get shop by ID", description = "Returns shop details")
    @GetMapping("/{id}")
    public ResponseEntity<ShopResponse> getShopById(@PathVariable UUID id) {
        return ResponseEntity.ok(shopService.getShopById(id));
    }
}

