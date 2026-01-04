package neyan.tech.baraka_backend.shop.repository;

import neyan.tech.baraka_backend.shop.entity.Shop;
import neyan.tech.baraka_backend.shop.entity.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    List<Shop> findByCreatedById(UUID userId);

    Page<Shop> findByStatus(ShopStatus status, Pageable pageable);

    Page<Shop> findByCity(String city, Pageable pageable);

    Page<Shop> findByCityAndStatus(String city, ShopStatus status, Pageable pageable);

    @Query("SELECT s FROM Shop s WHERE s.status = :status AND s.city = :city ORDER BY s.createdAt DESC")
    List<Shop> findActiveShopsInCity(@Param("city") String city, @Param("status") ShopStatus status);

    /**
     * Find active shops within a bounding box (for geo pre-filtering).
     * Uses indexed columns latitude and longitude.
     */
    @Query("""
            SELECT s FROM Shop s
            WHERE s.status = 'ACTIVE'
            AND s.latitude IS NOT NULL
            AND s.longitude IS NOT NULL
            AND s.latitude BETWEEN :minLat AND :maxLat
            AND s.longitude BETWEEN :minLng AND :maxLng
            ORDER BY s.createdAt DESC
            """)
    List<Shop> findActiveShopsInBoundingBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng
    );

    /**
     * Find active shops with available baskets within a bounding box.
     */
    @Query("""
            SELECT DISTINCT s FROM Shop s
            JOIN Basket b ON b.shop = s
            WHERE s.status = 'ACTIVE'
            AND s.latitude IS NOT NULL
            AND s.longitude IS NOT NULL
            AND s.latitude BETWEEN :minLat AND :maxLat
            AND s.longitude BETWEEN :minLng AND :maxLng
            AND b.status = 'PUBLISHED'
            AND b.quantityLeft > 0
            AND b.pickupEnd > :now
            ORDER BY s.createdAt DESC
            """)
    List<Shop> findActiveShopsWithBasketsInBoundingBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            @Param("now") Instant now
    );

    /**
     * Legacy method using native SQL Haversine (can be slower without index).
     */
    @Query(value = """
            SELECT s.* FROM shops s
            WHERE s.status = 'ACTIVE'
            AND s.latitude IS NOT NULL
            AND s.longitude IS NOT NULL
            AND (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude))
            * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat))
            * sin(radians(s.latitude)))) < :radiusKm
            ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude))
            * cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat))
            * sin(radians(s.latitude))))
            """, nativeQuery = true)
    List<Shop> findNearbyShops(@Param("lat") double latitude,
                               @Param("lng") double longitude,
                               @Param("radiusKm") double radiusKm);

    /**
     * Count shops in a city.
     */
    long countByCityAndStatus(String city, ShopStatus status);
}
