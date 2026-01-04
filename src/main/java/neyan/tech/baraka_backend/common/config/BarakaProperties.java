package neyan.tech.baraka_backend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "baraka")
public class BarakaProperties {

    private OrderProperties order = new OrderProperties();
    private BasketProperties basket = new BasketProperties();

    @Data
    public static class OrderProperties {
        private int cancelCutoffMinutes = 30;
        private int pickupCodeLength = 6;
    }

    @Data
    public static class BasketProperties {
        private int maxQuantityPerOrder = 5;
    }
}

