package org.example.QuanLyMuaVu.module.incident.service;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationMarkAllReadResponse;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationUnreadCountResponse;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationResponse;
import org.example.QuanLyMuaVu.module.incident.entity.Notification;
import org.example.QuanLyMuaVu.module.incident.repository.NotificationRepository;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private static final String DEFAULT_NOTIFICATION_TITLE = "Notification";
    private static final Duration EVENT_DUPLICATE_WINDOW = Duration.ofMinutes(2);

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<NotificationResponse> listCurrentUserNotifications() {
        Long userId = currentUserService.getCurrentUserId();
        return notificationRepository.findByUserIdOrderByNewest(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getCurrentUserUnreadCount() {
        Long userId = currentUserService.getCurrentUserId();
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        return NotificationUnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
    }

    public NotificationResponse markAsRead(Integer notificationId) {
        Long userId = currentUserService.getCurrentUserId();
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }

        return toResponse(notificationRepository.save(notification));
    }

    public NotificationMarkAllReadResponse markAllAsReadForCurrentUser() {
        Long userId = currentUserService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        int marked = notificationRepository.markAllAsReadByUserId(userId, now);
        return NotificationMarkAllReadResponse.builder()
                .markedCount(marked)
                .markedAt(now)
                .build();
    }

    public NotificationResponse createNotification(Long userId, String title, String message, String link) {
        return createNotificationInternal(userId, title, message, link, false);
    }

    public NotificationResponse createNotificationFromEvent(Long userId, String title, String message, String link) {
        return createNotificationInternal(userId, title, message, link, true);
    }

    private NotificationResponse createNotificationInternal(
            Long userId,
            String title,
            String message,
            String link,
            boolean deduplicateRecentEvent) {
        if (userId == null) {
            return null;
        }

        String normalizedTitle = normalizeTitle(title);
        String normalizedMessage = normalizeMessage(message);
        String normalizedLink = normalizeLink(link);

        if (deduplicateRecentEvent) {
            LocalDateTime fromTime = LocalDateTime.now().minus(EVENT_DUPLICATE_WINDOW);
            boolean duplicated = notificationRepository.existsRecentDuplicate(
                    userId,
                    normalizedTitle,
                    normalizedMessage,
                    normalizedLink,
                    fromTime);
            if (duplicated) {
                return null;
            }
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .title(normalizedTitle)
                .message(normalizedMessage)
                .link(normalizedLink)
                .build();
        return toResponse(notificationRepository.save(notification));
    }

    private String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return DEFAULT_NOTIFICATION_TITLE;
        }
        return title.trim();
    }

    private String normalizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        return message.trim();
    }

    private String normalizeLink(String link) {
        if (!StringUtils.hasText(link)) {
            return null;
        }
        return link.trim();
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .alertId(notification.getAlertId() != null
                        ? notification.getAlertId()
                        : notification.getAlert() != null ? notification.getAlert().getId() : null)
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
