package neyan.tech.ni3ma_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    private Long totalUsers;
    private Long totalShops;
    private Long totalBaskets;
    private Long totalOrders;
}
