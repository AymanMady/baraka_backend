package neyan.tech.ni3ma_backend.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.ni3ma_backend.review.dto.ReviewResponse;
import neyan.tech.ni3ma_backend.review.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/shops/{shopId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Shop Reviews", description = "Public shop review endpoints")
public class ShopReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Get shop reviews", description = "Returns paginated list of reviews for a shop")
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getShopReviews(
            @PathVariable UUID shopId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getShopReviews(shopId, pageable));
    }

    @Operation(summary = "Get shop average rating", description = "Returns average rating for a shop")
    @GetMapping("/stats")
    public ResponseEntity<ShopReviewStats> getShopReviewStats(@PathVariable UUID shopId) {
        Double avgRating = reviewService.getShopAverageRating(shopId);
        long count = reviewService.getShopReviewCount(shopId);
        return ResponseEntity.ok(new ShopReviewStats(avgRating, count));
    }

    public record ShopReviewStats(Double averageRating, Long reviewCount) {}
}

