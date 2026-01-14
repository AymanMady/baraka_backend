package neyan.tech.ni3ma_backend.favorite.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.common.security.CurrentUser;
import neyan.tech.ni3ma_backend.common.security.UserPrincipal;
import neyan.tech.ni3ma_backend.favorite.dto.FavoriteResponse;
import neyan.tech.ni3ma_backend.favorite.service.FavoriteService;
import neyan.tech.ni3ma_backend.shop.dto.ShopSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
@Tag(name = "Favorites", description = "Favorite shop endpoints")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "Add to favorites", description = "Adds a shop to user's favorites")
    @PostMapping("/{shopId}")
    public ResponseEntity<FavoriteResponse> addFavorite(
            @PathVariable UUID shopId,
            @CurrentUser UserPrincipal currentUser) {
        FavoriteResponse response = favoriteService.addFavorite(currentUser.getId(), shopId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Remove from favorites", description = "Removes a shop from user's favorites")
    @DeleteMapping("/{shopId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable UUID shopId,
            @CurrentUser UserPrincipal currentUser) {
        favoriteService.removeFavorite(currentUser.getId(), shopId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get my favorites", description = "Returns paginated list of favorite shops")
    @GetMapping
    public ResponseEntity<Page<ShopSummaryResponse>> getMyFavorites(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(favoriteService.getMyFavoriteShops(currentUser.getId(), pageable));
    }

    @Operation(summary = "Get favorite shop IDs", description = "Returns set of favorite shop IDs")
    @GetMapping("/ids")
    public ResponseEntity<Set<UUID>> getFavoriteShopIds(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(favoriteService.getFavoriteShopIds(currentUser.getId()));
    }

    @Operation(summary = "Check if favorite", description = "Checks if a shop is in favorites")
    @GetMapping("/{shopId}/check")
    public ResponseEntity<Boolean> isFavorite(
            @PathVariable UUID shopId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(favoriteService.isFavorite(currentUser.getId(), shopId));
    }

    @Operation(summary = "Toggle favorite", description = "Toggles shop favorite status")
    @PostMapping("/{shopId}/toggle")
    public ResponseEntity<FavoriteResponse> toggleFavorite(
            @PathVariable UUID shopId,
            @CurrentUser UserPrincipal currentUser) {
        FavoriteResponse response = favoriteService.toggleFavorite(currentUser.getId(), shopId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}

