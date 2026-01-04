package neyan.tech.baraka_backend.favorite.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.baraka_backend.common.exception.BadRequestException;
import neyan.tech.baraka_backend.common.exception.NotFoundException;
import neyan.tech.baraka_backend.favorite.dto.FavoriteResponse;
import neyan.tech.baraka_backend.favorite.entity.Favorite;
import neyan.tech.baraka_backend.favorite.entity.FavoriteId;
import neyan.tech.baraka_backend.favorite.mapper.FavoriteMapper;
import neyan.tech.baraka_backend.favorite.repository.FavoriteRepository;
import neyan.tech.baraka_backend.shop.dto.ShopSummaryResponse;
import neyan.tech.baraka_backend.shop.entity.Shop;
import neyan.tech.baraka_backend.shop.mapper.ShopMapper;
import neyan.tech.baraka_backend.shop.repository.ShopRepository;
import neyan.tech.baraka_backend.user.entity.User;
import neyan.tech.baraka_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final FavoriteMapper favoriteMapper;
    private final ShopMapper shopMapper;

    @Transactional
    public FavoriteResponse addFavorite(UUID userId, UUID shopId) {
        log.info("Adding shop {} to favorites for user {}", shopId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop", shopId));

        // Check if already favorited
        if (favoriteRepository.existsByUserIdAndShopId(userId, shopId)) {
            throw new BadRequestException("Shop is already in favorites");
        }

        FavoriteId favoriteId = new FavoriteId(userId, shopId);
        Favorite favorite = Favorite.builder()
                .id(favoriteId)
                .user(user)
                .shop(shop)
                .build();

        favorite = favoriteRepository.save(favorite);
        log.info("Shop {} added to favorites for user {}", shopId, userId);

        return favoriteMapper.toResponse(favorite);
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID shopId) {
        log.info("Removing shop {} from favorites for user {}", shopId, userId);

        if (!favoriteRepository.existsByUserIdAndShopId(userId, shopId)) {
            throw new NotFoundException("Favorite", "shopId", shopId);
        }

        favoriteRepository.deleteByUserIdAndShopId(userId, shopId);
        log.info("Shop {} removed from favorites for user {}", shopId, userId);
    }

    @Transactional(readOnly = true)
    public Page<ShopSummaryResponse> getMyFavoriteShops(UUID userId, Pageable pageable) {
        return favoriteRepository.findFavoriteShopsByUserId(userId, pageable)
                .map(shopMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getMyFavorites(UUID userId) {
        return favoriteMapper.toResponseList(favoriteRepository.findByUserId(userId));
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(UUID userId, UUID shopId) {
        return favoriteRepository.existsByUserIdAndShopId(userId, shopId);
    }

    @Transactional(readOnly = true)
    public Set<UUID> getFavoriteShopIds(UUID userId) {
        return favoriteRepository.findShopIdsByUserId(userId)
                .stream()
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public long getFavoriteCount(UUID shopId) {
        return favoriteRepository.countFavoritesForShop(shopId);
    }

    @Transactional
    public FavoriteResponse toggleFavorite(UUID userId, UUID shopId) {
        if (favoriteRepository.existsByUserIdAndShopId(userId, shopId)) {
            removeFavorite(userId, shopId);
            return null; // Indicates removal
        } else {
            return addFavorite(userId, shopId);
        }
    }
}

