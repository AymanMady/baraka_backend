package neyan.tech.baraka_backend.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import neyan.tech.baraka_backend.basket.dto.BasketResponse;
import neyan.tech.baraka_backend.order.entity.OrderStatus;
import neyan.tech.baraka_backend.user.dto.UserDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private UserDto user;
    private UUID basketId;
    private BasketResponse basket;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private String pickupCode;
    private Instant createdAt;
    private Instant updatedAt;
}

