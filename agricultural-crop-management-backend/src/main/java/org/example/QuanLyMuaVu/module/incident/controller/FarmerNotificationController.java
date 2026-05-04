package org.example.QuanLyMuaVu.module.incident.controller;


import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationMarkAllReadResponse;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationUnreadCountResponse;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationResponse;
import org.example.QuanLyMuaVu.module.incident.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farmer/notifications")
@RequiredArgsConstructor
public class FarmerNotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list() {
        return ApiResponse.success(notificationService.listCurrentUserNotifications());
    }

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountResponse> unreadCount() {
        return ApiResponse.success(notificationService.getCurrentUserUnreadCount());
    }

    @PreAuthorize("hasRole('FARMER')")
    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable("id") Integer id) {
        return ApiResponse.success(notificationService.markAsRead(id));
    }

    @PreAuthorize("hasRole('FARMER')")
    @PatchMapping("/read-all")
    public ApiResponse<NotificationMarkAllReadResponse> markAllRead() {
        return ApiResponse.success(notificationService.markAllAsReadForCurrentUser());
    }
}
