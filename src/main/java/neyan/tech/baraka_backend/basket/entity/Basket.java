package neyan.tech.baraka_backend.basket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import neyan.tech.baraka_backend.basket.entity.BasketImage;
import neyan.tech.baraka_backend.shop.entity.Shop;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "baskets", indexes = {
        @Index(name = "idx_baskets_shop_id", columnList = "shop_id"),
        @Index(name = "idx_baskets_status", columnList = "status"),
        @Index(name = "idx_baskets_status_pickup", columnList = "status, pickup_start"),
        @Index(name = "idx_baskets_pickup_start", columnList = "pickup_start")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Basket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @NotBlank
    @Size(min = 2, max = 150)
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @NotNull
    @Min(0)
    @Column(name = "price_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceOriginal;

    @NotNull
    @Min(0)
    @Column(name = "price_discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDiscount;

    @NotBlank
    @Size(max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "MRU";

    @NotNull
    @Min(1)
    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal;

    @NotNull
    @Min(0)
    @Column(name = "quantity_left", nullable = false)
    private Integer quantityLeft;

    @NotNull
    @Column(name = "pickup_start", nullable = false)
    private Instant pickupStart;

    @NotNull
    @Column(name = "pickup_end", nullable = false)
    private Instant pickupEnd;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BasketStatus status = BasketStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "basket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BasketImage> images = new ArrayList<>();
}

