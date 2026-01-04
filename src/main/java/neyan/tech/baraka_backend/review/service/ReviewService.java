package neyan.tech.baraka_backend.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.baraka_backend.common.exception.BadRequestException;
import neyan.tech.baraka_backend.common.exception.ForbiddenException;
import neyan.tech.baraka_backend.common.exception.NotFoundException;
import neyan.tech.baraka_backend.order.entity.Order;
import neyan.tech.baraka_backend.order.entity.OrderStatus;
import neyan.tech.baraka_backend.order.repository.OrderRepository;
import neyan.tech.baraka_backend.review.dto.CreateReviewRequest;
import neyan.tech.baraka_backend.review.dto.ReviewResponse;
import neyan.tech.baraka_backend.review.dto.UpdateReviewRequest;
import neyan.tech.baraka_backend.review.entity.Review;
import neyan.tech.baraka_backend.review.mapper.ReviewMapper;
import neyan.tech.baraka_backend.review.repository.ReviewRepository;
import neyan.tech.baraka_backend.user.entity.User;
import neyan.tech.baraka_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Create a review for a picked-up order
     */
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, UUID customerId) {
        log.info("Creating review for order {} by customer {}", request.getOrderId(), customerId);

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("User", customerId));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order", request.getOrderId()));

        // Verify customer owns the order
        if (!order.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("You can only review your own orders");
        }

        // Verify order was picked up
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new BadRequestException("You can only review orders that have been picked up");
        }

        // Check if review already exists
        if (reviewRepository.existsByOrderId(order.getId())) {
            throw new BadRequestException("A review already exists for this order");
        }

        Review review = Review.builder()
                .order(order)
                .shop(order.getBasket().getShop())
                .user(customer)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        log.info("Review created: {} for shop {}", review.getId(), order.getBasket().getShop().getId());

        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(UUID reviewId) {
        Review review = findReviewOrThrow(reviewId);
        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReviewByOrderId(UUID orderId) {
        Review review = reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Review", "orderId", orderId));
        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getShopReviews(UUID shopId, Pageable pageable) {
        return reviewRepository.findByShopId(shopId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(UUID customerId, Pageable pageable) {
        return reviewRepository.findByUserId(customerId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Double getShopAverageRating(UUID shopId) {
        return reviewRepository.getAverageRatingForShop(shopId);
    }

    @Transactional(readOnly = true)
    public long getShopReviewCount(UUID shopId) {
        return reviewRepository.countReviewsForShop(shopId);
    }

    @Transactional
    public ReviewResponse updateReview(UUID reviewId, UpdateReviewRequest request, UUID customerId) {
        Review review = findReviewOrThrow(reviewId);

        // Verify customer owns the review
        if (!review.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("You can only update your own reviews");
        }

        reviewMapper.updateEntity(request, review);
        review = reviewRepository.save(review);

        log.info("Review {} updated", reviewId);
        return reviewMapper.toResponse(review);
    }

    @Transactional
    public void deleteReview(UUID reviewId, UUID customerId) {
        Review review = findReviewOrThrow(reviewId);

        // Verify customer owns the review
        if (!review.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        log.info("Review {} deleted", reviewId);
    }

    // ==================== Internal Methods ====================

    private Review findReviewOrThrow(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", reviewId));
    }
}

