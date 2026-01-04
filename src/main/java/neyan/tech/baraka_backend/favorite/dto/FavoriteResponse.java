package neyan.tech.baraka_backend.favorite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import neyan.tech.baraka_backend.shop.dto.ShopSummaryResponse;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {

    private UUID userId;
    private UUID shopId;
    private ShopSummaryResponse shop;
    private Instant createdAt;
}

