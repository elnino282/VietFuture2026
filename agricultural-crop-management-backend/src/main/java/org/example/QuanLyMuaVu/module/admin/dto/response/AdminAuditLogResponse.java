package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminAuditLogResponse {

    Long id;
    String module;
    String operation;
    String entityType;
    Integer entityId;
    String performedBy;
    LocalDateTime performedAt;
    String snapshotData;
    String reason;
    String ipAddress;
}
