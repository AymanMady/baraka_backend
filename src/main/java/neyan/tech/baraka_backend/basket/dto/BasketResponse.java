package neyan.tech.baraka_backend.basket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import neyan.tech.baraka_backend.basket.entity.BasketStatus;
import neyan.tech.baraka_backend.shop.dto.ShopSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasketResponse {

    private UUID id;
    private UUID shopId;
    private ShopSummaryResponse shop;
    private String title;
    private String description;
    private BigDecimal priceOriginal;
    private BigDecimal priceDiscount;
    private String currency;
    private Integer quantityTotal;
    private Integer quantityLeft;
    private Instant pickupStart;
    private Instant pickupEnd;
    private BasketStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Computed
    private BigDecimal discountPercentage;

    // Images
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
}

