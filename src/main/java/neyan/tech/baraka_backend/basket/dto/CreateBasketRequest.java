package neyan.tech.baraka_backend.basket.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBasketRequest {

    @NotNull(message = "Shop ID is required")
    private UUID shopId;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 150, message = "Title must be between 2 and 150 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Original price is required")
    @Positive(message = "Original price must be positive")
    private BigDecimal priceOriginal;

    @NotNull(message = "Discount price is required")
    @Min(value = 0, message = "Discount price must be at least 0")
    private BigDecimal priceDiscount;

    @Size(max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer quantityTotal;

    @NotNull(message = "Pickup start time is required")
    @Future(message = "Pickup start time must be in the future")
    private Instant pickupStart;

    @NotNull(message = "Pickup end time is required")
    @Future(message = "Pickup end time must be in the future")
    private Instant pickupEnd;
}

