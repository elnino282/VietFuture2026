package org.example.farm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record FarmDocumentCreateRequest(
    @NotBlank(message = "Tiêu đề không được trống")
    String title,
    @NotNull(message = "Loại tài liệu không được trống")
    String documentType,
    String description,
    String fileUrl,
    LocalDate issuedDate,
    LocalDate expiryDate
) {}
