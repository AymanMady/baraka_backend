package neyan.tech.baraka_backend.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.baraka_backend.common.security.CurrentUser;
import neyan.tech.baraka_backend.common.security.UserPrincipal;
import neyan.tech.baraka_backend.notification.dto.NotificationResponse;
import neyan.tech.baraka_backend.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "User notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get my notifications", description = "Returns paginated list of user's notifications")
    @GetMapping("/my")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(notificationService.getMyNotifications(currentUser.getId(), pageable));
    }

    @Operation(summary = "Get unread notifications", description = "Returns paginated list of unread notifications")
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @CurrentUser UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(currentUser.getId(), pageable));
    }

    @Operation(summary = "Get unread count", description = "Returns count of unread notifications")
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(notificationService.getUnreadCount(currentUser.getId()));
    }

    @Operation(summary = "Mark as read", description = "Marks a notification as read")
    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser.getId()));
    }

    @Operation(summary = "Mark all as read", description = "Marks all notifications as read")
    @PostMapping("/read-all")
    public ResponseEntity<Integer> markAllAsRead(@CurrentUser UserPrincipal currentUser) {
        int updated = notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete notification", description = "Deletes a notification")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser) {
        notificationService.deleteNotification(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}

