package org.example.adminreporting.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
