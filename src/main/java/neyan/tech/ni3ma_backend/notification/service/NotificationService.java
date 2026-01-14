package neyan.tech.ni3ma_backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.ni3ma_backend.common.exception.ForbiddenException;
import neyan.tech.ni3ma_backend.common.exception.NotFoundException;
import neyan.tech.ni3ma_backend.notification.dto.NotificationResponse;
import neyan.tech.ni3ma_backend.notification.entity.Notification;
import neyan.tech.ni3ma_backend.notification.entity.NotificationType;
import neyan.tech.ni3ma_backend.notification.mapper.NotificationMapper;
import neyan.tech.ni3ma_backend.notification.repository.NotificationRepository;
import neyan.tech.ni3ma_backend.user.entity.User;
import neyan.tech.ni3ma_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    /**
     * Create a new notification for a user
     */
    @Transactional
    public NotificationResponse createNotification(UUID userId, String title, String body, NotificationType type) {
        log.debug("Creating notification for user {}: {}", userId, title);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .type(type)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created: {} for user {}", notification.getId(), userId);

        return notificationMapper.toResponse(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = findNotificationOrThrow(notificationId);
        checkNotificationAccess(notification, userId);

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);

        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        int updated = notificationRepository.markAllAsReadForUser(userId);
        log.info("Marked {} notifications as read for user {}", updated, userId);
        return updated;
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = findNotificationOrThrow(notificationId);
        checkNotificationAccess(notification, userId);

        notificationRepository.delete(notification);
        log.info("Notification {} deleted", notificationId);
    }

    /**
     * Delete old notifications (cleanup job)
     */
    @Transactional
    public int deleteOldNotifications(int daysOld) {
        Instant threshold = Instant.now().minus(daysOld, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteOldNotifications(threshold);
        log.info("Deleted {} old notifications", deleted);
        return deleted;
    }

    // ==================== Notification Helpers ====================

    public void notifyOrderConfirmed(UUID userId, String shopName, String pickupCode) {
        createNotification(
                userId,
                "Commande confirmée !",
                String.format("Votre commande chez %s est confirmée. Code de retrait: %s", shopName, pickupCode),
                NotificationType.ORDER_CONFIRMED
        );
    }

    public void notifyOrderReady(UUID userId, String shopName) {
        createNotification(
                userId,
                "Commande prête !",
                String.format("Votre commande chez %s est prête à être récupérée.", shopName),
                NotificationType.ORDER_READY
        );
    }

    public void notifyOrderCancelled(UUID userId, String shopName) {
        createNotification(
                userId,
                "Commande annulée",
                String.format("Votre commande chez %s a été annulée.", shopName),
                NotificationType.ORDER_CANCELLED
        );
    }

    public void notifyPaymentReceived(UUID userId, String amount, String currency) {
        createNotification(
                userId,
                "Paiement reçu",
                String.format("Votre paiement de %s %s a été confirmé.", amount, currency),
                NotificationType.PAYMENT_RECEIVED
        );
    }

    public void notifyPromotion(UUID userId, String title, String body) {
        createNotification(userId, title, body, NotificationType.PROMOTION);
    }

    // ==================== Internal Methods ====================

    private Notification findNotificationOrThrow(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification", notificationId));
    }

    private void checkNotificationAccess(Notification notification, UUID userId) {
        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You don't have access to this notification");
        }
    }
}

