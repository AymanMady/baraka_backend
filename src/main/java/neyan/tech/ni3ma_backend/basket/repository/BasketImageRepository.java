package neyan.tech.ni3ma_backend.basket.repository;

import neyan.tech.ni3ma_backend.basket.entity.BasketImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BasketImageRepository extends JpaRepository<BasketImage, UUID> {

    List<BasketImage> findByBasketIdOrderByDisplayOrderAsc(UUID basketId);

    void deleteByBasketId(UUID basketId);
}
