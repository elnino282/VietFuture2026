package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pending approval item shown on admin dashboard.
 * All fields are backed by real domain data, never placeholders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPendingApprovalItemDTO {
    private Long id;
    private String type;
    private String title;
    private String subtitle;
    private LocalDateTime submittedAt;
    private String priority;
    private String severity;
    private String actionUrl;
    private String actionTarget;
}
