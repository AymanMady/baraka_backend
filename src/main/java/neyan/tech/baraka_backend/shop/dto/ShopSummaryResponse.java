package neyan.tech.baraka_backend.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import neyan.tech.baraka_backend.shop.entity.ShopStatus;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSummaryResponse {

    private UUID id;
    private String name;
    private String city;
    private ShopStatus status;
    private Double averageRating;
}

