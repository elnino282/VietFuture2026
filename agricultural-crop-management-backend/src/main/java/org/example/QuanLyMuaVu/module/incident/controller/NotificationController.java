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
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list() {
        return ApiResponse.success(notificationService.listCurrentUserNotifications());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountResponse> unreadCount() {
        return ApiResponse.success(notificationService.getCurrentUserUnreadCount());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable("id") Integer id) {
        return ApiResponse.success(notificationService.markAsRead(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BUYER','FARMER','EMPLOYEE')")
    @PatchMapping("/read-all")
    public ApiResponse<NotificationMarkAllReadResponse> markAllRead() {
        return ApiResponse.success(notificationService.markAllAsReadForCurrentUser());
    }
}
