package neyan.tech.ni3ma_backend.favorite.mapper;

import neyan.tech.ni3ma_backend.favorite.dto.FavoriteResponse;
import neyan.tech.ni3ma_backend.favorite.entity.Favorite;
import neyan.tech.ni3ma_backend.shop.mapper.ShopMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ShopMapper.class})
public interface FavoriteMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shop", source = "shop")
    FavoriteResponse toResponse(Favorite favorite);

    List<FavoriteResponse> toResponseList(List<Favorite> favorites);
}

