package neyan.tech.baraka_backend.basket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.baraka_backend.basket.dto.BasketResponse;
import neyan.tech.baraka_backend.basket.dto.CreateBasketRequest;
import neyan.tech.baraka_backend.basket.dto.UpdateBasketRequest;
import neyan.tech.baraka_backend.basket.entity.Basket;
import neyan.tech.baraka_backend.basket.entity.BasketImage;
import neyan.tech.baraka_backend.basket.entity.BasketStatus;
import neyan.tech.baraka_backend.basket.mapper.BasketMapper;
import neyan.tech.baraka_backend.basket.repository.BasketImageRepository;
import neyan.tech.baraka_backend.basket.repository.BasketRepository;
import neyan.tech.baraka_backend.common.exception.NotFoundException;
import neyan.tech.baraka_backend.common.service.ImageStorageService;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasketService {

    private final BasketRepository basketRepository;
    private final BasketImageRepository basketImageRepository;
    private final BasketMapper basketMapper;
    private final ShopService shopService;
    private final ImageStorageService imageStorageService;

    @Transactional
    public BasketResponse createBasket(CreateBasketRequest request, UUID merchantId) {
        log.info("Creating basket for shop: {} by merchant: {}", request.getShopId(), merchantId);

        Shop shop = shopService.findShopOrThrow(request.getShopId());
        log.debug("Shop found: {} with status: {}", shop.getId(), shop.getStatus());
        
        shopService.checkShopOwnership(shop, merchantId);

        if (shop.getStatus() != ShopStatus.ACTIVE) {
            log.warn("Attempted to create basket for non-active shop: {} with status: {}", shop.getId(), shop.getStatus());
            throw new BadRequestException("Cannot create basket for non-active shop");
        }

        validateBasketTimes(request.getPickupStart(), request.getPickupEnd());
        validateBasketPrices(request.getPriceOriginal(), request.getPriceDiscount());
        log.debug("Basket validation passed for shop: {}", request.getShopId());

        Basket basket = basketMapper.toEntity(request);
        basket.setShop(shop);
        basket.setQuantityLeft(request.getQuantityTotal());
        basket.setStatus(BasketStatus.DRAFT);

        // Ensure currency is set correctly (override mapper default if needed)
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            basket.setCurrency("MRU");
            log.debug("Currency set to default: MRU");
        } else {
            basket.setCurrency(request.getCurrency());
        }
        
        // Ensure shop entity is properly initialized for mapping
        // The shop should already be loaded, but verify basic fields are accessible
        log.debug("Shop entity loaded - id: {}, name: {}, city: {}, status: {}", 
                shop.getId(), shop.getName(), shop.getCity(), shop.getStatus());

        // Verify basket entity is properly initialized before saving
        if (basket.getShop() == null) {
            log.error("Shop is null in basket before save for shop: {}", shop.getId());
            throw new BadRequestException("Shop must be set on basket");
        }
        
        try {
            log.debug("Attempting to save basket with title: {}, priceOriginal: {}, priceDiscount: {}, pickupStart: {}, pickupEnd: {}, shopId: {}", 
                    basket.getTitle(), basket.getPriceOriginal(), basket.getPriceDiscount(), 
                    basket.getPickupStart(), basket.getPickupEnd(), basket.getShop().getId());
            basket = basketRepository.save(basket);
            log.info("Basket created successfully with id: {} for shop: {}", basket.getId(), shop.getId());
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.error("Data integrity violation when saving basket for shop: {}. Error: {}", shop.getId(), ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to save basket for shop: {}. Exception type: {}, Error: {}", 
                    shop.getId(), ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }

        try {
            log.debug("Mapping basket to response for basket id: {}", basket.getId());
            
            // Ensure shop entity is still accessible and fully loaded
            Shop basketShop = basket.getShop();
            if (basketShop == null) {
                log.error("Shop is null in basket after save. Basket id: {}", basket.getId());
                throw new BadRequestException("Shop relationship lost after save");
            }
            
            // Access shop properties to ensure they're loaded (prevent lazy loading issues)
            log.debug("Shop details - id: {}, name: {}, city: {}, status: {}", 
                    basketShop.getId(), basketShop.getName(), basketShop.getCity(), basketShop.getStatus());
            
            BasketResponse response = basketMapper.toResponse(basket);
            log.debug("Successfully mapped basket to response for basket id: {}", basket.getId());
            
            // Verify response is valid
            if (response == null) {
                log.error("BasketResponse is null after mapping for basket id: {}", basket.getId());
                throw new IllegalStateException("Failed to map basket to response");
            }
            
            log.debug("Response created successfully - basket id: {}, shop id: {}", 
                    response.getId(), response.getShopId());
            
            return response;
        } catch (org.hibernate.LazyInitializationException ex) {
            log.error("Lazy initialization exception when mapping basket id: {}. Error: {}", basket.getId(), ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to map basket to response for basket id: {}. Exception type: {}, Error: {}", 
                    basket.getId(), ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public BasketResponse getBasketById(UUID basketId) {
        Basket basket = findBasketOrThrow(basketId);
        // Load images eagerly for response
        basket.getImages().size(); // Force lazy loading
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

        // Delete image files
        imageStorageService.deleteBasketImages(basketId);

        basketRepository.delete(basket);
        log.info("Basket deleted: {}", basketId);
    }

    @Transactional
    public BasketResponse addImagesToBasket(UUID basketId, List<String> imageUrls, UUID merchantId) {
        Basket basket = findBasketOrThrow(basketId);
        shopService.checkShopOwnership(basket.getShop(), merchantId);

        int displayOrder = basket.getImages().size();
        for (String imageUrl : imageUrls) {
            BasketImage image = BasketImage.builder()
                    .basket(basket)
                    .imageUrl(imageUrl)
                    .displayOrder(displayOrder++)
                    .build();
            basketImageRepository.save(image);
        }

        basket = basketRepository.findById(basketId).orElseThrow();
        basket.getImages().size(); // Force lazy loading
        log.info("Added {} images to basket {}", imageUrls.size(), basketId);
        return basketMapper.toResponse(basket);
    }

    @Transactional
    public BasketResponse removeImageFromBasket(UUID basketId, UUID imageId, UUID merchantId) {
        Basket basket = findBasketOrThrow(basketId);
        shopService.checkShopOwnership(basket.getShop(), merchantId);

        BasketImage image = basketImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("BasketImage", imageId));

        if (!image.getBasket().getId().equals(basketId)) {
            throw new BadRequestException("Image does not belong to this basket");
        }

        // Delete file
        imageStorageService.deleteImage(image.getImageUrl());

        // Delete from database
        basketImageRepository.delete(image);

        basket = basketRepository.findById(basketId).orElseThrow();
        basket.getImages().size(); // Force lazy loading
        log.info("Removed image {} from basket {}", imageId, basketId);
        return basketMapper.toResponse(basket);
    }

    @Transactional
    public BasketResponse uploadBasketImages(UUID basketId, MultipartFile[] files, UUID merchantId) {
        Basket basket = findBasketOrThrow(basketId);
        shopService.checkShopOwnership(basket.getShop(), merchantId);

        // Store images and get URLs
        List<String> imageUrls = imageStorageService.storeBasketImages(basketId, files);

        // Save image records
        return addImagesToBasket(basketId, imageUrls, merchantId);
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
        if (pickupStart == null || pickupEnd == null) {
            throw new BadRequestException("Pickup start and end times are required");
        }
        
        Instant now = Instant.now();
        
        // Check if pickup start is in the future
        if (!pickupStart.isAfter(now)) {
            throw new BadRequestException("Pickup start time must be in the future");
        }
        
        // Check if pickup end is after pickup start
        if (!pickupEnd.isAfter(pickupStart)) {
            throw new BadRequestException("Pickup end must be after pickup start");
        }
        
        // Check if pickup end is in the future
        if (!pickupEnd.isAfter(now)) {
            throw new BadRequestException("Pickup end time must be in the future");
        }
    }

    private void validateBasketPrices(java.math.BigDecimal original, java.math.BigDecimal discount) {
        if (original == null || discount == null) {
            throw new BadRequestException("Both original and discount prices are required");
        }
        
        if (original.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Original price must be greater than zero");
        }
        
        if (discount.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Discount price cannot be negative");
        }
        
        if (discount.compareTo(original) > 0) {
            throw new BadRequestException("Discount price cannot be greater than original price");
        }
    }
}

