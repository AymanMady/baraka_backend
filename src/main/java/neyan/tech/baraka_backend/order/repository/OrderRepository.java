package neyan.tech.baraka_backend.order.repository;

import neyan.tech.baraka_backend.order.entity.Order;
import neyan.tech.baraka_backend.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByPickupCode(String pickupCode);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    List<Order> findByBasketId(UUID basketId);

    @Query("SELECT o FROM Order o WHERE o.basket.shop.id = :shopId")
    Page<Order> findByShopId(@Param("shopId") UUID shopId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.basket.shop.id = :shopId AND o.status = :status")
    Page<Order> findByShopIdAndStatus(@Param("shopId") UUID shopId,
                                      @Param("status") OrderStatus status,
                                      Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.basket.id = :basketId AND o.status NOT IN ('CANCELLED')")
    long countActiveOrdersForBasket(@Param("basketId") UUID basketId);

    @Query("SELECT SUM(o.quantity) FROM Order o WHERE o.basket.id = :basketId AND o.status NOT IN ('CANCELLED')")
    Integer sumQuantityForBasket(@Param("basketId") UUID basketId);

    @Query("SELECT o FROM Order o WHERE o.status = 'RESERVED' AND o.basket.pickupEnd < :now")
    List<Order> findExpiredReservations(@Param("now") Instant now);

    boolean existsByPickupCode(String pickupCode);
}

