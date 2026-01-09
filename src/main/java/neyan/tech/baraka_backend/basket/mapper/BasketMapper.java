package neyan.tech.baraka_backend.basket.mapper;

import neyan.tech.baraka_backend.basket.dto.BasketResponse;
import neyan.tech.baraka_backend.basket.dto.CreateBasketRequest;
import neyan.tech.baraka_backend.basket.dto.UpdateBasketRequest;
import neyan.tech.baraka_backend.basket.entity.Basket;
import neyan.tech.baraka_backend.shop.mapper.ShopMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ShopMapper.class})
public interface BasketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "quantityLeft", source = "quantityTotal")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "currency", defaultValue = "MRU")
    Basket toEntity(CreateBasketRequest request);

    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shop", source = "shop")
    @Mapping(target = "discountPercentage", ignore = true)
    BasketResponse toResponse(Basket basket);

    List<BasketResponse> toResponseList(List<Basket> baskets);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "quantityLeft", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateBasketRequest request, @MappingTarget Basket basket);

    @AfterMapping
    default void calculateDiscountPercentage(@MappingTarget BasketResponse response, Basket basket) {
        if (basket.getPriceOriginal() != null && basket.getPriceOriginal().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = basket.getPriceOriginal().subtract(basket.getPriceDiscount());
            BigDecimal percentage = discount.multiply(BigDecimal.valueOf(100))
                    .divide(basket.getPriceOriginal(), 2, RoundingMode.HALF_UP);
            response.setDiscountPercentage(percentage);
        }
    }
}

