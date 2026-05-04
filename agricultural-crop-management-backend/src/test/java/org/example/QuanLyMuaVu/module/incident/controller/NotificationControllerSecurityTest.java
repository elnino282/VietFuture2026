package org.example.QuanLyMuaVu.module.incident.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.incident.dto.response.NotificationResponse;
import org.example.QuanLyMuaVu.module.incident.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NotificationController.class)
@Import(NotificationControllerSecurityTest.MethodSecurityTestConfig.class)
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void list_withAllowedRole_returnsNotifications() throws Exception {
        when(notificationService.listCurrentUserNotifications()).thenReturn(List.of(
                NotificationResponse.builder()
                        .id(9)
                        .title("Alert")
                        .message("test")
                        .createdAt(LocalDateTime.now())
                        .build()));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(9));
    }

    @Test
    void list_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST")
    void list_withUnsupportedRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void markRead_whenNotOwner_returnsNotFound() throws Exception {
        when(notificationService.markAsRead(404))
                .thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/notifications/404/read").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ERR_RESOURCE_NOT_FOUND"));
    }
}
