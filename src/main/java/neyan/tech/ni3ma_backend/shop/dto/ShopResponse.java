package neyan.tech.ni3ma_backend.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import neyan.tech.ni3ma_backend.shop.entity.ShopStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopResponse {

    private UUID id;
    private String name;
    private String description;
    private String phone;
    private String address;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private ShopStatus status;
    private UUID createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;

    // Computed fields
    private Double averageRating;
    private Long reviewCount;
    private Long favoriteCount;
}

