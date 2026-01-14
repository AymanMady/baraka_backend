package neyan.tech.ni3ma_backend.basket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.ni3ma_backend.basket.dto.BasketResponse;
import neyan.tech.ni3ma_backend.basket.dto.CreateBasketRequest;
import neyan.tech.ni3ma_backend.basket.dto.UpdateBasketRequest;
import neyan.tech.ni3ma_backend.basket.service.BasketService;
import neyan.tech.ni3ma_backend.common.security.CurrentUser;
import neyan.tech.ni3ma_backend.common.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
@Tag(name = "Merchant - Baskets", description = "Merchant basket management endpoints")
public class MerchantBasketController {

    private final BasketService basketService;

    @Operation(summary = "Create a new basket", description = "Creates a new basket for a shop")
    @PostMapping("/shops/{shopId}/baskets")
    public ResponseEntity<BasketResponse> createBasket(
            @PathVariable UUID shopId,
            @Valid @RequestBody CreateBasketRequest request,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("Received basket creation request for shop: {} from user: {}", shopId, currentUser.getId());
            log.debug("Request data: title={}, priceOriginal={}, priceDiscount={}, quantityTotal={}, pickupStart={}, pickupEnd={}",
                    request.getTitle(), request.getPriceOriginal(), request.getPriceDiscount(),
                    request.getQuantityTotal(), request.getPickupStart(), request.getPickupEnd());
            
            request.setShopId(shopId);
            BasketResponse response = basketService.createBasket(request, currentUser.getId());
            
            log.info("Basket created successfully with id: {} for shop: {}", response.getId(), shopId);
            log.debug("Response basket: id={}, shopId={}, shop={}", response.getId(), response.getShopId(), response.getShop());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating basket for shop: {} by user: {}", shopId, currentUser.getId(), ex);
            // Re-throw to let GlobalExceptionHandler handle it
            throw ex;
        }
    }

    @Operation(summary = "Get my baskets for a shop", description = "Returns all baskets for a shop")
    @GetMapping("/shops/{shopId}/baskets")
    public ResponseEntity<List<BasketResponse>> getMyBaskets(
            @PathVariable UUID shopId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(basketService.getMyBaskets(shopId, currentUser.getId()));
    }

    @Operation(summary = "Update basket", description = "Updates basket details (only draft baskets)")
    @PutMapping("/baskets/{id}")
    public ResponseEntity<BasketResponse> updateBasket(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBasketRequest request,
            @CurrentUser UserPrincipal currentUser) {
        BasketResponse response = basketService.updateBasket(id, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Publish basket", description = "Publishes a draft basket")
    @PostMapping("/baskets/{id}/publish")
    public ResponseEntity<BasketResponse> publishBasket(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        BasketResponse response = basketService.publishBasket(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Unpublish basket", description = "Unpublishes a basket back to draft")
    @PostMapping("/baskets/{id}/unpublish")
    public ResponseEntity<BasketResponse> unpublishBasket(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        BasketResponse response = basketService.unpublishBasket(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete basket", description = "Deletes a draft basket")
    @DeleteMapping("/baskets/{id}")
    public ResponseEntity<Void> deleteBasket(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        basketService.deleteBasket(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload basket images", description = "Uploads one or more images for a basket")
    @PostMapping(value = "/baskets/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<BasketResponse> uploadBasketImages(
            @PathVariable UUID id,
            @RequestParam("files") MultipartFile[] files,
            @CurrentUser UserPrincipal currentUser) {
        try {
            log.info("Received image upload request for basket: {} from user: {}, files: {}", id, currentUser.getId(), files.length);
            
            // Detailed logging for debugging
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    MultipartFile file = files[i];
                    log.debug("File {}: name={}, size={}, contentType={}, isEmpty={}", 
                        i, file.getOriginalFilename(), file.getSize(), file.getContentType(), file.isEmpty());
                }
            } else {
                log.warn("Files array is null or empty");
            }
            
            BasketResponse response = basketService.uploadBasketImages(id, files, currentUser.getId());
            log.info("Images uploaded successfully for basket: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error uploading images for basket: {} by user: {}", id, currentUser.getId(), ex);
            log.error("Exception type: {}, message: {}", ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    @Operation(summary = "Delete basket image", description = "Deletes a specific image from a basket")
    @DeleteMapping("/baskets/{id}/images/{imageId}")
    public ResponseEntity<BasketResponse> deleteBasketImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @CurrentUser UserPrincipal currentUser) {
        BasketResponse response = basketService.removeImageFromBasket(id, imageId, currentUser.getId());
        return ResponseEntity.ok(response);
    }
}

