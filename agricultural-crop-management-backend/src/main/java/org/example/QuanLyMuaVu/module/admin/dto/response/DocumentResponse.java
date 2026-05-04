package org.example.QuanLyMuaVu.module.admin.dto.response;

import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentResponse {
    Integer documentId;
    String title;
    String url;
    String description;
    String crop;
    String stage;
    String topic;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Boolean isFavorited;
}
