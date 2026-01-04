package neyan.tech.baraka_backend.review.repository;

import neyan.tech.baraka_backend.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByOrderId(UUID orderId);

    Page<Review> findByShopId(UUID shopId, Pageable pageable);

    Page<Review> findByUserId(UUID userId, Pageable pageable);

    boolean existsByOrderId(UUID orderId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.shop.id = :shopId")
    Double getAverageRatingForShop(@Param("shopId") UUID shopId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.shop.id = :shopId")
    long countReviewsForShop(@Param("shopId") UUID shopId);

    @Query("SELECT r FROM Review r WHERE r.shop.id = :shopId ORDER BY r.createdAt DESC")
    Page<Review> findLatestReviewsForShop(@Param("shopId") UUID shopId, Pageable pageable);
}

