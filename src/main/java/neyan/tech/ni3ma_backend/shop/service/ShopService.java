package neyan.tech.ni3ma_backend.shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.ni3ma_backend.common.exception.ForbiddenException;
import neyan.tech.ni3ma_backend.common.exception.NotFoundException;
import neyan.tech.ni3ma_backend.review.repository.ReviewRepository;
import neyan.tech.ni3ma_backend.favorite.repository.FavoriteRepository;
import neyan.tech.ni3ma_backend.shop.dto.CreateShopRequest;
import neyan.tech.ni3ma_backend.shop.dto.ShopResponse;
import neyan.tech.ni3ma_backend.shop.dto.UpdateShopRequest;
import neyan.tech.ni3ma_backend.shop.entity.Shop;
import neyan.tech.ni3ma_backend.shop.entity.ShopStatus;
import neyan.tech.ni3ma_backend.shop.mapper.ShopMapper;
import neyan.tech.ni3ma_backend.shop.repository.ShopRepository;
import neyan.tech.ni3ma_backend.user.entity.User;
import neyan.tech.ni3ma_backend.user.entity.UserRole;
import neyan.tech.ni3ma_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final ShopMapper shopMapper;

    @Transactional
    public ShopResponse createShop(CreateShopRequest request, UUID merchantId) {
        log.info("Creating shop for merchant: {}", merchantId);

        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("User", merchantId));

        Shop shop = shopMapper.toEntity(request);
        shop.setCreatedBy(merchant);
        shop.setStatus(ShopStatus.PENDING);

        shop = shopRepository.save(shop);
        log.info("Shop created with id: {}", shop.getId());

        return enrichShopResponse(shopMapper.toResponse(shop));
    }

    @Transactional(readOnly = true)
    public ShopResponse getShopById(UUID shopId) {
        Shop shop = findShopOrThrow(shopId);
        return enrichShopResponse(shopMapper.toResponse(shop));
    }

    @Transactional(readOnly = true)
    public Page<ShopResponse> getActiveShops(Pageable pageable) {
        return shopRepository.findByStatus(ShopStatus.ACTIVE, pageable)
                .map(shop -> enrichShopResponse(shopMapper.toResponse(shop)));
    }

    @Transactional(readOnly = true)
    public Page<ShopResponse> getShopsByCity(String city, Pageable pageable) {
        return shopRepository.findByCityAndStatus(city, ShopStatus.ACTIVE, pageable)
                .map(shop -> enrichShopResponse(shopMapper.toResponse(shop)));
    }

    @Transactional(readOnly = true)
    public Page<ShopResponse> getMyShops(UUID merchantId, Pageable pageable) {
        return shopRepository.findByCreatedById(merchantId).stream()
                .map(shop -> enrichShopResponse(shopMapper.toResponse(shop)))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())));
    }

    @Transactional
    public ShopResponse updateShop(UUID shopId, UpdateShopRequest request, UUID userId) {
        Shop shop = findShopOrThrow(shopId);
        checkShopOwnership(shop, userId);

        shopMapper.updateEntity(request, shop);
        shop = shopRepository.save(shop);

        log.info("Shop updated: {}", shopId);
        return enrichShopResponse(shopMapper.toResponse(shop));
    }

    @Transactional
    public ShopResponse activateShop(UUID shopId) {
        Shop shop = findShopOrThrow(shopId);
        shop.setStatus(ShopStatus.ACTIVE);
        shop = shopRepository.save(shop);

        log.info("Shop activated: {}", shopId);
        return enrichShopResponse(shopMapper.toResponse(shop));
    }

    @Transactional
    public ShopResponse suspendShop(UUID shopId) {
        Shop shop = findShopOrThrow(shopId);
        shop.setStatus(ShopStatus.SUSPENDED);
        shop = shopRepository.save(shop);

        log.info("Shop suspended: {}", shopId);
        return enrichShopResponse(shopMapper.toResponse(shop));
    }

    public Shop findShopOrThrow(UUID shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop", shopId));
    }

    public void checkShopOwnership(Shop shop, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        if (user.getRole() == UserRole.ADMIN) {
            return; // Admin can edit any shop
        }

        if (!shop.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("You can only modify your own shops");
        }
    }

    private ShopResponse enrichShopResponse(ShopResponse response) {
        Double avgRating = reviewRepository.getAverageRatingForShop(response.getId());
        long reviewCount = reviewRepository.countReviewsForShop(response.getId());
        long favoriteCount = favoriteRepository.countFavoritesForShop(response.getId());

        response.setAverageRating(avgRating);
        response.setReviewCount(reviewCount);
        response.setFavoriteCount(favoriteCount);

        return response;
    }
}

