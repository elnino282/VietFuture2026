package org.example.QuanLyMuaVu.module.incident.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationMarkAllReadResponse;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationResponse;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationUnreadCountResponse;
import org.example.QuanLyMuaVu.module.incident.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FarmerNotificationController.class)
@Import(FarmerNotificationControllerTest.MethodSecurityTestConfig.class)
class FarmerNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "FARMER")
    @DisplayName("Farmer can view own notifications")
    void list_WithFarmerRole_ReturnsSuccess() throws Exception {
        when(notificationService.listCurrentUserNotifications()).thenReturn(List.of(
                NotificationResponse.builder()
                        .id(1)
                        .title("Incident reported")
                        .message("Severity HIGH incident requires follow-up.")
                        .createdAt(LocalDateTime.now())
                        .build()
        ));

        mockMvc.perform(get("/api/v1/farmer/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(1))
                .andExpect(jsonPath("$.result[0].title").value("Incident reported"));
    }

    @Test
    @WithMockUser(roles = "BUYER")
    @DisplayName("Non-farmer cannot access farmer notification endpoint")
    void list_WithNonFarmerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/farmer/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FARMER")
    @DisplayName("Farmer can fetch unread count")
    void unreadCount_WithFarmerRole_ReturnsSuccess() throws Exception {
        when(notificationService.getCurrentUserUnreadCount())
                .thenReturn(NotificationUnreadCountResponse.builder().unreadCount(4L).build());

        mockMvc.perform(get("/api/v1/farmer/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.unreadCount").value(4));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    @DisplayName("Farmer can mark all notifications as read")
    void markAllRead_WithFarmerRole_ReturnsSuccess() throws Exception {
        when(notificationService.markAllAsReadForCurrentUser())
                .thenReturn(NotificationMarkAllReadResponse.builder().markedCount(2).build());

        mockMvc.perform(patch("/api/v1/farmer/notifications/read-all").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.markedCount").value(2));
    }
}
