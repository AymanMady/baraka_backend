package neyan.tech.baraka_backend.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import neyan.tech.baraka_backend.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

    private UUID id;
    private String basketTitle;
    private String shopName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private String pickupCode;
    private Instant pickupStart;
    private Instant pickupEnd;
    private Instant createdAt;
}

