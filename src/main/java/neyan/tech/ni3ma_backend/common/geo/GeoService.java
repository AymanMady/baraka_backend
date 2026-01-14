package neyan.tech.ni3ma_backend.common.geo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.ni3ma_backend.basket.dto.BasketResponse;
import neyan.tech.ni3ma_backend.basket.entity.Basket;
import neyan.tech.ni3ma_backend.basket.entity.BasketStatus;
import neyan.tech.ni3ma_backend.basket.mapper.BasketMapper;
import neyan.tech.ni3ma_backend.basket.repository.BasketRepository;
import neyan.tech.ni3ma_backend.shop.dto.ShopResponse;
import neyan.tech.ni3ma_backend.shop.entity.Shop;
import neyan.tech.ni3ma_backend.shop.entity.ShopStatus;
import neyan.tech.ni3ma_backend.shop.mapper.ShopMapper;
import neyan.tech.ni3ma_backend.shop.repository.ShopRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for geographic searches using Haversine distance calculation.
 * Uses bounding box pre-filtering for optimization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoService {

    private final ShopRepository shopRepository;
    private final BasketRepository basketRepository;
    private final ShopMapper shopMapper;
    private final BasketMapper basketMapper;

    /**
     * Find shops near a location, sorted by distance.
     *
     * @param lat      Center latitude
     * @param lng      Center longitude
     * @param radiusKm Search radius in kilometers
     * @param pageable Pagination parameters
     * @return Page of ShopResponse with distance information
     */
    @Transactional(readOnly = true)
    public Page<ShopWithDistance> findNearbyShops(double lat, double lng, double radiusKm, Pageable pageable) {
        log.debug("Searching shops near ({}, {}) within {} km", lat, lng, radiusKm);

        // Calculate bounding box for pre-filtering
        GeoUtils.BoundingBox bbox = GeoUtils.calculateBoundingBox(lat, lng, radiusKm);

        // Query with bounding box filter
        List<Shop> shopsInBox = shopRepository.findActiveShopsInBoundingBox(
                bbox.minLatDecimal(),
                bbox.maxLatDecimal(),
                bbox.minLngDecimal(),
                bbox.maxLngDecimal()
        );

        log.debug("Found {} shops in bounding box", shopsInBox.size());

        // Calculate exact distance and filter by radius
        List<ShopWithDistance> nearbyShops = shopsInBox.stream()
                .filter(shop -> shop.getLatitude() != null && shop.getLongitude() != null)
                .map(shop -> {
                    double distance = GeoUtils.haversineDistance(
                            lat, lng,
                            shop.getLatitude().doubleValue(),
                            shop.getLongitude().doubleValue()
                    );
                    return new ShopWithDistance(shopMapper.toResponse(shop), distance);
                })
                .filter(result -> result.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(ShopWithDistance::distanceKm))
                .collect(Collectors.toList());

        log.debug("Found {} shops within {} km radius", nearbyShops.size(), radiusKm);

        // Apply pagination
        return paginateList(nearbyShops, pageable);
    }

    /**
     * Find baskets near a location, sorted by distance.
     *
     * @param lat      Center latitude
     * @param lng      Center longitude
     * @param radiusKm Search radius in kilometers
     * @param pageable Pagination parameters
     * @return Page of BasketWithDistance
     */
    @Transactional(readOnly = true)
    public Page<BasketWithDistance> findNearbyBaskets(double lat, double lng, double radiusKm, Pageable pageable) {
        log.debug("Searching baskets near ({}, {}) within {} km", lat, lng, radiusKm);

        // Calculate bounding box for pre-filtering
        GeoUtils.BoundingBox bbox = GeoUtils.calculateBoundingBox(lat, lng, radiusKm);

        // Query baskets with shop in bounding box
        List<Basket> basketsInBox = basketRepository.findAvailableBasketsInBoundingBox(
                bbox.minLatDecimal(),
                bbox.maxLatDecimal(),
                bbox.minLngDecimal(),
                bbox.maxLngDecimal(),
                Instant.now()
        );

        log.debug("Found {} baskets in bounding box", basketsInBox.size());

        // Calculate exact distance and filter by radius
        List<BasketWithDistance> nearbyBaskets = basketsInBox.stream()
                .filter(basket -> basket.getShop().getLatitude() != null && basket.getShop().getLongitude() != null)
                .map(basket -> {
                    double distance = GeoUtils.haversineDistance(
                            lat, lng,
                            basket.getShop().getLatitude().doubleValue(),
                            basket.getShop().getLongitude().doubleValue()
                    );
                    return new BasketWithDistance(basketMapper.toResponse(basket), distance);
                })
                .filter(result -> result.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(BasketWithDistance::distanceKm))
                .collect(Collectors.toList());

        log.debug("Found {} baskets within {} km radius", nearbyBaskets.size(), radiusKm);

        // Apply pagination
        return paginateList(nearbyBaskets, pageable);
    }

    /**
     * Find shops with available baskets near a location.
     */
    @Transactional(readOnly = true)
    public Page<ShopWithDistance> findShopsWithAvailableBaskets(double lat, double lng, double radiusKm, Pageable pageable) {
        log.debug("Searching shops with baskets near ({}, {}) within {} km", lat, lng, radiusKm);

        GeoUtils.BoundingBox bbox = GeoUtils.calculateBoundingBox(lat, lng, radiusKm);

        List<Shop> shopsInBox = shopRepository.findActiveShopsWithBasketsInBoundingBox(
                bbox.minLatDecimal(),
                bbox.maxLatDecimal(),
                bbox.minLngDecimal(),
                bbox.maxLngDecimal(),
                Instant.now()
        );

        List<ShopWithDistance> nearbyShops = shopsInBox.stream()
                .filter(shop -> shop.getLatitude() != null && shop.getLongitude() != null)
                .map(shop -> {
                    double distance = GeoUtils.haversineDistance(
                            lat, lng,
                            shop.getLatitude().doubleValue(),
                            shop.getLongitude().doubleValue()
                    );
                    return new ShopWithDistance(shopMapper.toResponse(shop), distance);
                })
                .filter(result -> result.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(ShopWithDistance::distanceKm))
                .collect(Collectors.toList());

        return paginateList(nearbyShops, pageable);
    }

    /**
     * Helper method to paginate a list.
     */
    private <T> Page<T> paginateList(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        if (start > list.size()) {
            return new PageImpl<>(List.of(), pageable, list.size());
        }

        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    /**
     * Record for shop with distance.
     */
    public record ShopWithDistance(
            ShopResponse shop,
            double distanceKm
    ) {}

    /**
     * Record for basket with distance.
     */
    public record BasketWithDistance(
            BasketResponse basket,
            double distanceKm
    ) {}
}

