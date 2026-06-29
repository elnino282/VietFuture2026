package org.example.adminreporting.controller;

import lombok.RequiredArgsConstructor;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.AdminDocumentResponse;
import org.example.adminreporting.service.AdminDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {

    private final AdminDocumentService adminDocumentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminDocumentResponse>>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        PageResponse<AdminDocumentResponse> response = adminDocumentService.listDocuments(page, size, q, type, status, sort);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> getDocumentById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(adminDocumentService.getDocumentById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> createDocument(
            @RequestBody CreateDocumentRequest request) {
        AdminDocumentResponse response = adminDocumentService.createDocument(
                request.title(),
                request.description(),
                request.documentUrl(),
                request.documentType(),
                request.status());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> updateDocument(
            @PathVariable Integer id,
            @RequestBody CreateDocumentRequest request) {
        AdminDocumentResponse response = adminDocumentService.updateDocument(
                id,
                request.title(),
                request.description(),
                request.documentUrl(),
                request.documentType(),
                request.status());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDeleteDocument(@PathVariable Integer id) {
        adminDocumentService.softDeleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> hardDeleteDocument(@PathVariable Integer id) {
        adminDocumentService.hardDeleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    record CreateDocumentRequest(
            String title,
            String description,
            String documentUrl,
            String documentType,
            String status) {
    }
}
