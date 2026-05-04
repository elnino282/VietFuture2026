package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;
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
public class AdminAlertResponse {
    Integer id;
    String type;
    String severity;
    String status;
    Integer farmId;
    String farmName;
    Integer seasonId;
    Integer plotId;
    Integer cropId;
    String title;
    String message;
    String suggestedActionType;
    String suggestedActionUrl;
    List<Integer> recipientFarmerIds;
    LocalDateTime createdAt;
    LocalDateTime sentAt;
}
