package org.example.QuanLyMuaVu.module.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for creating a new document.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDocumentCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description;

    @NotBlank(message = "Document URL is required")
    @Size(max = 1024, message = "Document URL must not exceed 1024 characters")
    @Pattern(regexp = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", message = "Must be a valid URL")
    String documentUrl;

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "^(POLICY|GUIDE|MANUAL|LEGAL|OTHER)$", message = "Document type must be one of: POLICY, GUIDE, MANUAL, LEGAL, OTHER")
    String documentType;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE")
    String status;
}
