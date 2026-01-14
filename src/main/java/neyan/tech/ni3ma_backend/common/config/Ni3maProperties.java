package neyan.tech.ni3ma_backend.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ni3ma")
public class Ni3maProperties {

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

