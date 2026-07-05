package org.example.template.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TemplateResponse {
    private String id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
