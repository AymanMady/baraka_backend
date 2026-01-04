package neyan.tech.baraka_backend.review.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import neyan.tech.baraka_backend.common.security.CurrentUser;
import neyan.tech.baraka_backend.common.security.UserPrincipal;
import neyan.tech.baraka_backend.review.dto.CreateReviewRequest;
import neyan.tech.baraka_backend.review.dto.ReviewResponse;
import neyan.tech.baraka_backend.review.dto.UpdateReviewRequest;
import neyan.tech.baraka_backend.review.service.ReviewService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review endpoints")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Create review", description = "Creates a review for a picked-up order")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @CurrentUser UserPrincipal currentUser) {
        ReviewResponse response = reviewService.createReview(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get review by ID", description = "Returns review details")
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @Operation(summary = "Get my reviews", description = "Returns paginated list of user's reviews")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getMyReviews(currentUser.getId(), pageable));
    }

    @Operation(summary = "Update review", description = "Updates a review")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewRequest request,
            @CurrentUser UserPrincipal currentUser) {
        ReviewResponse response = reviewService.updateReview(id, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete review", description = "Deletes a review")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        reviewService.deleteReview(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}

