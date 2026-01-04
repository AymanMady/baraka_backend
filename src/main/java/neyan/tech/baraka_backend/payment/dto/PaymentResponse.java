package neyan.tech.baraka_backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import neyan.tech.baraka_backend.payment.entity.PaymentProvider;
import neyan.tech.baraka_backend.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private PaymentProvider provider;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;
}

