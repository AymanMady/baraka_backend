package neyan.tech.baraka_backend.basket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.baraka_backend.basket.dto.BasketResponse;
import neyan.tech.baraka_backend.basket.dto.CreateBasketRequest;
import neyan.tech.baraka_backend.basket.dto.UpdateBasketRequest;
import neyan.tech.baraka_backend.basket.entity.Basket;
import neyan.tech.baraka_backend.basket.entity.BasketStatus;
import neyan.tech.baraka_backend.basket.mapper.BasketMapper;
import neyan.tech.baraka_backend.basket.repository.BasketRepository;
import neyan.tech.baraka_backend.common.exception.BadRequestException;
import neyan.tech.baraka_backend.common.exception.ForbiddenException;
import neyan.tech.baraka_backend.common.exception.NotFoundException;
import neyan.tech.baraka_backend.shop.entity.Shop;
import neyan.tech.baraka_backend.shop.entity.ShopStatus;
import neyan.tech.baraka_backend.shop.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasketService {

    private final BasketRepository basketRepository;
    private final BasketMapper basketMapper;
    private final ShopService shopService;

    @Transactional
    public BasketResponse createBasket(CreateBasketRequest request, UUID merchantId) {
        log.info("Creating basket for shop: {}", request.getShopId());

        Shop shop = shopService.findShopOrThrow(request.getShopId());
        shopService.checkShopOwnership(shop, merchantId);

        if (shop.getStatus() != ShopStatus.ACTIVE) {
            throw new BadRequestException("Cannot create basket for non-active shop");
        }

        validateBasketTimes(request.getPickupStart(), request.getPickupEnd());
        validateBasketPrices(request.getPriceOriginal(), request.getPriceDiscount());

        Basket basket = basketMapper.toEntity(request);
        basket.setShop(shop);
        basket.setQuantityLeft(request.getQuantityTotal());
        basket.setStatus(BasketStatus.DRAFT);

        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            basket.setCurrency("XOF");
        }

        basket = basketRepository.save(basket);
        log.info("Basket created with id: {}", basket.getId());

        return basketMapper.toResponse(basket);
    }

    @Transactional(readOnly = true)
    public BasketResponse getBasketById(UUID basketId) {
        Basket basket = findBasketOrThrow(basketId);
        return basketMapper.toResponse(basket);
    }

    @Transactional(readOnly = true)
    public Page<BasketResponse> getAvailableBaskets(Pageable pageable) {
        return basketRepository.findByStatus(BasketStatus.PUBLISHED, pageable)
                .map(basketMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BasketResponse> getBasketsByShop(UUID shopId, Pageable pageable) {
        return basketRepository.findByShopIdAndStatus(shopId, BasketStatus.PUBLISHED, pageable)
                .map(basketMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<BasketResponse> getAvailableBasketsForShop(UUID shopId) {
        return basketMapper.toResponseList(
                basketRepository.findAvailableBasketsForShop(shopId, Instant.now())
        );
    }

    @Transactional(readOnly = true)
    public List<BasketResponse> getMyBaskets(UUID shopId, UUID merchantId) {
        Shop shop = shopService.findShopOrThrow(shopId);
        shopService.checkShopOwnership(shop, merchantId);

        return basketMapper.toResponseList(basketRepository.findByShopId(shopId));
    }

    @Transactional
    public BasketResponse updateBasket(UUID basketId, UpdateBasketRequest request, UUID merchantId) {
        Basket basket = findBasketOrThrow(basketId);
        shopService.checkShopOwnership(basket.getShop(), merchantId);

        if (basket.getStatus() != BasketStatus.DRAFT) {
            throw new BadRequestException("Cannot update a published basket");
        }

        if (request.getPickupStart() != null || request.getPickupEnd() != null) {
            Instant start = request.getPickupStart() != null ? request.getPickupStart() : basket.getPickupStart();
            Instant end = request.getPickupEnd() != null ? request.getPickupEnd() : basket.getPickupEnd();
            validateBasketTimes(start, end);
        }

        if (request.getPriceOriginal() != null || request.getPriceDiscount() != null) {
            var original = request.getPriceOriginal() != null ? request.getPriceOriginal() : basket.getPriceOriginal();
            var discount = request.getPriceDiscount() != null ? request.getPriceDiscount() : basket.getPriceDiscount();
            validateBasketPrices(original, discount);
        }

        basketMapper.updateEntity(request, basket);

        // Update quantityLeft if quantityTotal changed
        if (request.getQuantityTotal() != null) {
            basket.setQuantityLeft(request.getQuantityTotal());
        }

        basket = basketRepository.save(basket);
        log.info("Basket updated: {}", basketId);

        return basketMapper.toResponse(basket);
    }

    @Transactional
    public BasketResponse publishBasket(UUID basketId, UUID merchantId) {
        Basket basket = findBasketOrThrow(basketId);
        shopService.checkShopOwnership(basket.getShop(), merchantId);

        if (basket.getStatus() != BasketStatus.DRAFT) {
            throw new BadRequestException("Only draft baskets can be published");
        }

        if (basket.getPickupEnd().isBefore(Instant.now())) {
            throw new BadRequestException("Cannot publish basket with past pickup end time");
        }

        basket.setStatus(BasketStatus.PUBLISHED);
        basket = basketRepository.save(basket);

        log.info("Basket published: {}", basketId);
        return basketMapper.toResponse(basket);
    }

    @Transactional
    public BasketResponse unpublishBasket(UUID basketId, UUID merchantId) {
        Basket basket = findBasketOrThrow(basketId);
        shopService.checkShopOwnership(basket.getShop(), merchantId);

        if (basket.getStatus() != BasketStatus.PUBLISHED) {
            throw new BadRequestException("Only published baskets can be unpublished");
        }

        basket.setStatus(BasketStatus.DRAFT);
        basket = basketRepository.save(basket);

        log.info("Basket unpublished: {}", basketId);
        return basketMapper.toResponse(basket);
    }

    @Transactional
    public void deleteBasket(UUID basketId, UUID merchantId) {
        Basket basket = findBasketOrThrow(basketId);
        shopService.checkShopOwnership(basket.getShop(), merchantId);

        if (basket.getStatus() == BasketStatus.PUBLISHED) {
            throw new BadRequestException("Cannot delete a published basket. Unpublish it first.");
        }

        basketRepository.delete(basket);
        log.info("Basket deleted: {}", basketId);
    }

    // ==================== Internal Methods ====================

    public Basket findBasketOrThrow(UUID basketId) {
        return basketRepository.findById(basketId)
                .orElseThrow(() -> new NotFoundException("Basket", basketId));
    }

    @Transactional
    public void decrementQuantity(Basket basket, int quantity) {
        int newQuantityLeft = basket.getQuantityLeft() - quantity;
        if (newQuantityLeft < 0) {
            throw new BadRequestException("Not enough quantity available");
        }

        basket.setQuantityLeft(newQuantityLeft);

        if (newQuantityLeft == 0) {
            basket.setStatus(BasketStatus.SOLD_OUT);
            log.info("Basket {} is now sold out", basket.getId());
        }

        basketRepository.save(basket);
    }

    @Transactional
    public void incrementQuantity(Basket basket, int quantity) {
        basket.setQuantityLeft(basket.getQuantityLeft() + quantity);

        // If basket was sold_out and now has quantity, republish if not expired
        if (basket.getStatus() == BasketStatus.SOLD_OUT && basket.getQuantityLeft() > 0) {
            if (basket.getPickupEnd().isAfter(Instant.now())) {
                basket.setStatus(BasketStatus.PUBLISHED);
                log.info("Basket {} is republished after cancellation", basket.getId());
            }
        }

        basketRepository.save(basket);
    }

    // ==================== Scheduled Tasks ====================

    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void expireBaskets() {
        int expired = basketRepository.expireBaskets(Instant.now());
        if (expired > 0) {
            log.info("Expired {} baskets", expired);
        }
    }

    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void markSoldOutBaskets() {
        int soldOut = basketRepository.markSoldOutBaskets();
        if (soldOut > 0) {
            log.info("Marked {} baskets as sold out", soldOut);
        }
    }

    // ==================== Validation ====================

    private void validateBasketTimes(Instant pickupStart, Instant pickupEnd) {
        if (pickupStart != null && pickupEnd != null) {
            if (!pickupEnd.isAfter(pickupStart)) {
                throw new BadRequestException("Pickup end must be after pickup start");
            }
        }
    }

    private void validateBasketPrices(java.math.BigDecimal original, java.math.BigDecimal discount) {
        if (original != null && discount != null) {
            if (discount.compareTo(original) > 0) {
                throw new BadRequestException("Discount price cannot be greater than original price");
            }
        }
    }
}

