package neyan.tech.ni3ma_backend.notification.mapper;

import neyan.tech.ni3ma_backend.notification.dto.CreateNotificationRequest;
import neyan.tech.ni3ma_backend.notification.dto.NotificationResponse;
import neyan.tech.ni3ma_backend.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Notification toEntity(CreateNotificationRequest request);

    @Mapping(target = "userId", source = "user.id")
    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}

