package neyan.tech.baraka_backend.shop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.baraka_backend.shop.dto.ShopResponse;
import neyan.tech.baraka_backend.shop.entity.ShopStatus;
import neyan.tech.baraka_backend.shop.service.ShopService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
// @SecurityRequirement(name = "bearerAuth")
// @PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Shops", description = "Admin shop management endpoints")
public class AdminShopController {

    private final ShopService shopService;

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

