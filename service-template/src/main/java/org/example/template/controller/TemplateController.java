package org.example.template.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.template.entity.TemplateRecord;
import org.example.template.repository.TemplateRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Template APIs", description = "Endpoints for managing template records and demonstrating template structure")
public class TemplateController {

    private final TemplateRecordRepository repository;

    @GetMapping("/public/hello")
    @Operation(summary = "Public endpoint", description = "Demonstrates a public endpoint that does not require authorization header")
    public ResponseEntity<String> getPublicHello() {
        return ResponseEntity.ok("Hello! This is a public endpoint from ACM Service Template.");
    }

    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'FARMER')")
    @Operation(summary = "Create template record", description = "Authorized endpoint to create a new template record. Requires ADMIN or FARMER role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<TemplateRecord> createRecord(@Valid @RequestBody CreateRecordRequest request) {
        TemplateRecord record = TemplateRecord.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        TemplateRecord saved = repository.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'FARMER', 'EMPLOYEE')")
    @Operation(summary = "List all template records", description = "Authorized endpoint to list all records. Requires ADMIN, FARMER, or EMPLOYEE role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<TemplateRecord>> listRecords() {
        return ResponseEntity.ok(repository.findAll());
    }

    @Data
    public static class CreateRecordRequest {
        @NotBlank(message = "Name is required")
        private String name;
        private String description;
    }
}
