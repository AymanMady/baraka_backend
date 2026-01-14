package neyan.tech.ni3ma_backend.favorite.repository;

import neyan.tech.ni3ma_backend.favorite.entity.Favorite;
import neyan.tech.ni3ma_backend.favorite.entity.FavoriteId;
import neyan.tech.ni3ma_backend.shop.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    List<Favorite> findByUserId(UUID userId);

    @Query("SELECT f.shop FROM Favorite f WHERE f.user.id = :userId")
    Page<Shop> findFavoriteShopsByUserId(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByUserIdAndShopId(UUID userId, UUID shopId);

    void deleteByUserIdAndShopId(UUID userId, UUID shopId);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.shop.id = :shopId")
    long countFavoritesForShop(@Param("shopId") UUID shopId);

    @Query("SELECT f.shop.id FROM Favorite f WHERE f.user.id = :userId")
    List<UUID> findShopIdsByUserId(@Param("userId") UUID userId);
}

