package neyan.tech.ni3ma_backend.shop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.common.security.CurrentUser;
import neyan.tech.ni3ma_backend.common.security.UserPrincipal;
import neyan.tech.ni3ma_backend.shop.dto.CreateShopRequest;
import neyan.tech.ni3ma_backend.shop.dto.ShopResponse;
import neyan.tech.ni3ma_backend.shop.dto.UpdateShopRequest;
import neyan.tech.ni3ma_backend.shop.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/merchant/shops")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
@Tag(name = "Merchant - Shops", description = "Merchant shop management endpoints")
public class MerchantShopController {

    private final ShopService shopService;

    @Operation(summary = "Create a new shop", description = "Creates a new shop for the merchant")
    @PostMapping
    public ResponseEntity<ShopResponse> createShop(
            @Valid @RequestBody CreateShopRequest request,
            @CurrentUser UserPrincipal currentUser) {
        ShopResponse response = shopService.createShop(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get my shops", description = "Returns all shops owned by the merchant")
    @GetMapping
    public ResponseEntity<Page<ShopResponse>> getMyShops(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(shopService.getMyShops(currentUser.getId(), pageable));
    }

    @Operation(summary = "Update shop", description = "Updates shop details")
    @PutMapping("/{id}")
    public ResponseEntity<ShopResponse> updateShop(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShopRequest request,
            @CurrentUser UserPrincipal currentUser) {
        ShopResponse response = shopService.updateShop(id, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}

