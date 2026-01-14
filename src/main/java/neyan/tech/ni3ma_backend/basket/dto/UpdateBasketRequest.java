package neyan.tech.ni3ma_backend.basket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import neyan.tech.ni3ma_backend.basket.entity.BasketStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBasketRequest {

    @Size(min = 2, max = 150, message = "Title must be between 2 and 150 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Positive(message = "Original price must be positive")
    private BigDecimal priceOriginal;

    @Min(value = 0, message = "Discount price must be at least 0")
    private BigDecimal priceDiscount;

    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer quantityTotal;

    private Instant pickupStart;

    private Instant pickupEnd;

    private BasketStatus status;
}

