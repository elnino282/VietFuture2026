package org.example.QuanLyMuaVu.module.admin.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO for document data.
 * Field names match frontend Zod schema exactly.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDocumentResponse {
    Long id;
    String title;
    String description;
    String documentUrl;
    String documentType;
    String status;
    String createdAt;
    String updatedAt;
    Long createdBy;
}
