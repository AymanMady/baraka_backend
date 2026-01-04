package neyan.tech.baraka_backend.basket.repository;

import neyan.tech.baraka_backend.basket.entity.Basket;
import neyan.tech.baraka_backend.basket.entity.BasketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BasketRepository extends JpaRepository<Basket, UUID> {

    List<Basket> findByShopId(UUID shopId);

    Page<Basket> findByShopIdAndStatus(UUID shopId, BasketStatus status, Pageable pageable);

    Page<Basket> findByStatus(BasketStatus status, Pageable pageable);

    @Query("SELECT b FROM Basket b WHERE b.status = :status AND b.pickupStart >= :start AND b.pickupStart <= :end")
    List<Basket> findAvailableBaskets(@Param("status") BasketStatus status,
                                      @Param("start") Instant start,
                                      @Param("end") Instant end);

    @Query("SELECT b FROM Basket b WHERE b.shop.id = :shopId AND b.status = 'PUBLISHED' AND b.quantityLeft > 0 AND b.pickupEnd > :now")
    List<Basket> findAvailableBasketsForShop(@Param("shopId") UUID shopId, @Param("now") Instant now);

    /**
     * Find available baskets within a bounding box (for geo pre-filtering).
     * Joins with shop to get location data.
     */
    @Query("""
            SELECT b FROM Basket b
            JOIN FETCH b.shop s
            WHERE b.status = 'PUBLISHED'
            AND b.quantityLeft > 0
            AND b.pickupEnd > :now
            AND s.status = 'ACTIVE'
            AND s.latitude IS NOT NULL
            AND s.longitude IS NOT NULL
            AND s.latitude BETWEEN :minLat AND :maxLat
            AND s.longitude BETWEEN :minLng AND :maxLng
            ORDER BY b.pickupStart ASC
            """)
    List<Basket> findAvailableBasketsInBoundingBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("now") Instant now
    );

    /**
     * Find available baskets with pickup time filter.
     */
    @Query("""
            SELECT b FROM Basket b
            WHERE b.status = 'PUBLISHED'
            AND b.quantityLeft > 0
            AND b.pickupEnd > :now
            AND b.pickupStart >= :fromTime
            AND b.pickupStart <= :toTime
            ORDER BY b.pickupStart ASC
            """)
    Page<Basket> findAvailableBasketsWithTimeFilter(
            @Param("now") Instant now,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Basket b SET b.status = 'EXPIRED' WHERE b.status = 'PUBLISHED' AND b.pickupEnd < :now")
    int expireBaskets(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Basket b SET b.status = 'SOLD_OUT' WHERE b.status = 'PUBLISHED' AND b.quantityLeft = 0")
    int markSoldOutBaskets();

    /**
     * Count available baskets for a shop.
     */
    @Query("SELECT COUNT(b) FROM Basket b WHERE b.shop.id = :shopId AND b.status = 'PUBLISHED' AND b.quantityLeft > 0 AND b.pickupEnd > :now")
    long countAvailableBasketsForShop(@Param("shopId") UUID shopId, @Param("now") Instant now);
}
