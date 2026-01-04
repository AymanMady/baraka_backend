package neyan.tech.baraka_backend.shop.mapper;

import neyan.tech.baraka_backend.shop.dto.CreateShopRequest;
import neyan.tech.baraka_backend.shop.dto.ShopResponse;
import neyan.tech.baraka_backend.shop.dto.ShopSummaryResponse;
import neyan.tech.baraka_backend.shop.dto.UpdateShopRequest;
import neyan.tech.baraka_backend.shop.entity.Shop;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShopMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Shop toEntity(CreateShopRequest request);

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    @Mapping(target = "favoriteCount", ignore = true)
    ShopResponse toResponse(Shop shop);

    ShopSummaryResponse toSummaryResponse(Shop shop);

    List<ShopResponse> toResponseList(List<Shop> shops);

    List<ShopSummaryResponse> toSummaryList(List<Shop> shops);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateShopRequest request, @MappingTarget Shop shop);
}

