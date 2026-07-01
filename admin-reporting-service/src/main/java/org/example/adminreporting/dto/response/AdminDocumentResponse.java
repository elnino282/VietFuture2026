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
    // Primary Fields (required by frontend schema)
    Long documentId;
    String title;
    String url;
    String description;
    
    // Filtering Fields
    String crop;
    String stage;
    String topic;
    String documentType;
    
    // Metadata
    Integer viewCount;
    Boolean isPinned;
    Boolean isActive;
    
    // Timestamps
    String createdAt;
    String updatedAt;
    
    // User tracking
    Long createdBy;
    Boolean isFavorited;  // Per-user, defaults to false
    Boolean isPublic;     // Can be used to control visibility
}

