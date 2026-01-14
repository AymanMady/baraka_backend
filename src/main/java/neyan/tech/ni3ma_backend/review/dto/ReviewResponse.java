package neyan.tech.ni3ma_backend.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID orderId;
    private UUID shopId;
    private String shopName;
    private UUID userId;
    private String userName;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}

