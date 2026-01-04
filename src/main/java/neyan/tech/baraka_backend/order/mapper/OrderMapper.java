package neyan.tech.baraka_backend.order.mapper;

import neyan.tech.baraka_backend.basket.mapper.BasketMapper;
import neyan.tech.baraka_backend.order.dto.CreateOrderRequest;
import neyan.tech.baraka_backend.order.dto.OrderResponse;
import neyan.tech.baraka_backend.order.dto.OrderSummaryResponse;
import neyan.tech.baraka_backend.order.entity.Order;
import neyan.tech.baraka_backend.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class, BasketMapper.class})
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "basket", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "pickupCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "basketId", source = "basket.id")
    @Mapping(target = "basket", source = "basket")
    OrderResponse toResponse(Order order);

    @Mapping(target = "basketTitle", source = "basket.title")
    @Mapping(target = "shopName", source = "basket.shop.name")
    @Mapping(target = "pickupStart", source = "basket.pickupStart")
    @Mapping(target = "pickupEnd", source = "basket.pickupEnd")
    OrderSummaryResponse toSummaryResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    List<OrderSummaryResponse> toSummaryList(List<Order> orders);
}

