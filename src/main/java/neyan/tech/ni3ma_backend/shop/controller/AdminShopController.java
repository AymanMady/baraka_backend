package neyan.tech.ni3ma_backend.shop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.shop.dto.ShopResponse;
import neyan.tech.ni3ma_backend.shop.entity.Shop;
import neyan.tech.ni3ma_backend.shop.entity.ShopStatus;
import neyan.tech.ni3ma_backend.shop.mapper.ShopMapper;
import neyan.tech.ni3ma_backend.shop.repository.ShopRepository;
import neyan.tech.ni3ma_backend.shop.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Shops", description = "Admin shop management endpoints")
public class AdminShopController {

    private final ShopService shopService;
    private final ShopRepository shopRepository;
    private final ShopMapper shopMapper;

    @Operation(summary = "Get all shops", description = "Returns paginated list of all shops (any status)")
    @GetMapping
    public ResponseEntity<Page<ShopResponse>> getAllShops(
            @RequestParam(required = false) ShopStatus status,
            @RequestParam(required = false) String city,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Shop> shops;
        if (status != null && city != null) {
            shops = shopRepository.findByCityAndStatus(city, status, pageable);
        } else if (status != null) {
            shops = shopRepository.findByStatus(status, pageable);
        } else if (city != null) {
            shops = shopRepository.findByCity(city, pageable);
        } else {
            shops = shopRepository.findAll(pageable);
        }

        return ResponseEntity.ok(shops.map(shopMapper::toResponse));
    }

    @Operation(summary = "Get shop by ID", description = "Returns shop details")
    @GetMapping("/{id}")
    public ResponseEntity<ShopResponse> getShopById(@PathVariable UUID id) {
        return ResponseEntity.ok(shopService.getShopById(id));
    }

    @Operation(summary = "Update shop status", description = "Activates or suspends a shop")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ShopResponse> updateShopStatus(
            @PathVariable UUID id,
            @RequestParam ShopStatus status) {
        ShopResponse response;
        if (status == ShopStatus.ACTIVE) {
            response = shopService.activateShop(id);
        } else if (status == ShopStatus.SUSPENDED) {
            response = shopService.suspendShop(id);
        } else {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        return ResponseEntity.ok(response);
    }
}

