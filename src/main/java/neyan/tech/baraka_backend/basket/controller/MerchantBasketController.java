package neyan.tech.baraka_backend.basket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import neyan.tech.baraka_backend.basket.dto.BasketResponse;
import neyan.tech.baraka_backend.basket.dto.CreateBasketRequest;
import neyan.tech.baraka_backend.basket.dto.UpdateBasketRequest;
import neyan.tech.baraka_backend.basket.service.BasketService;
import neyan.tech.baraka_backend.common.security.CurrentUser;
import neyan.tech.baraka_backend.common.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
@Tag(name = "Merchant - Baskets", description = "Merchant basket management endpoints")
public class MerchantBasketController {

    private final BasketService basketService;

    @Operation(summary = "Create a new basket", description = "Creates a new basket for a shop")
    @PostMapping("/shops/{shopId}/baskets")
    public ResponseEntity<BasketResponse> createBasket(
            @PathVariable UUID shopId,
            @Valid @RequestBody CreateBasketRequest request,
            @CurrentUser UserPrincipal currentUser) {
        request.setShopId(shopId);
        BasketResponse response = basketService.createBasket(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get my baskets for a shop", description = "Returns all baskets for a shop")
    @GetMapping("/shops/{shopId}/baskets")
    public ResponseEntity<List<BasketResponse>> getMyBaskets(
            @PathVariable UUID shopId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(basketService.getMyBaskets(shopId, currentUser.getId()));
    }

    @Operation(summary = "Update basket", description = "Updates basket details (only draft baskets)")
    @PutMapping("/baskets/{id}")
    public ResponseEntity<BasketResponse> updateBasket(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBasketRequest request,
            @CurrentUser UserPrincipal currentUser) {
        BasketResponse response = basketService.updateBasket(id, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Publish basket", description = "Publishes a draft basket")
    @PostMapping("/baskets/{id}/publish")
    public ResponseEntity<BasketResponse> publishBasket(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        BasketResponse response = basketService.publishBasket(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Unpublish basket", description = "Unpublishes a basket back to draft")
    @PostMapping("/baskets/{id}/unpublish")
    public ResponseEntity<BasketResponse> unpublishBasket(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        BasketResponse response = basketService.unpublishBasket(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete basket", description = "Deletes a draft basket")
    @DeleteMapping("/baskets/{id}")
    public ResponseEntity<Void> deleteBasket(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        basketService.deleteBasket(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}

