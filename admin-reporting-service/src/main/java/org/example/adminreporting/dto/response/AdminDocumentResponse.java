package org.example.adminreporting.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
