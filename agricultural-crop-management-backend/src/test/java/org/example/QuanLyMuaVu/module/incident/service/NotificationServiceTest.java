package org.example.QuanLyMuaVu.module.incident.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationMarkAllReadResponse;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationResponse;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationUnreadCountResponse;
import org.example.QuanLyMuaVu.module.incident.entity.Notification;
import org.example.QuanLyMuaVu.module.incident.repository.NotificationRepository;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("createNotificationFromEvent saves a notification when not duplicated")
    void createNotificationFromEvent_WhenUnique_SavesNotification() {
        when(notificationRepository.existsRecentDuplicate(anyLong(), any(), any(), any(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification entity = invocation.getArgument(0);
                    entity.setId(1001);
                    return entity;
                });

        NotificationResponse response = notificationService.createNotificationFromEvent(
                77L,
                "Task assigned",
                "Task 'Prepare field' has been assigned to you.",
                "/seasons/1/tasks/22");

        assertEquals(1001, response.getId());
        assertEquals("Task assigned", response.getTitle());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("createNotificationFromEvent skips duplicate in dedupe window")
    void createNotificationFromEvent_WhenDuplicate_Skips() {
        when(notificationRepository.existsRecentDuplicate(eq(77L), any(), any(), any(), any(LocalDateTime.class)))
                .thenReturn(true);

        NotificationResponse response = notificationService.createNotificationFromEvent(
                77L,
                "Task assigned",
                "Task 'Prepare field' has been assigned to you.",
                "/seasons/1/tasks/22");

        assertNull(response);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead rejects notification from another user")
    void markAsRead_WhenNotOwned_ThrowsNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(55L);
        when(notificationRepository.findByIdAndUserId(999, 55L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> notificationService.markAsRead(999));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("unread count and mark-all-read operate on current user only")
    void unreadCountAndMarkAllRead_ForCurrentUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(55L);
        when(notificationRepository.countUnreadByUserId(55L)).thenReturn(3L);
        when(notificationRepository.markAllAsReadByUserId(eq(55L), any(LocalDateTime.class))).thenReturn(3);

        NotificationUnreadCountResponse unreadCount = notificationService.getCurrentUserUnreadCount();
        NotificationMarkAllReadResponse markAllRead = notificationService.markAllAsReadForCurrentUser();

        assertEquals(3L, unreadCount.getUnreadCount());
        assertEquals(3, markAllRead.getMarkedCount());
        verify(notificationRepository).countUnreadByUserId(55L);
        verify(notificationRepository).markAllAsReadByUserId(eq(55L), any(LocalDateTime.class));
    }
}
