package neyan.tech.ni3ma_backend.user.mapper;

import neyan.tech.ni3ma_backend.user.dto.UserDto;
import neyan.tech.ni3ma_backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);
}

