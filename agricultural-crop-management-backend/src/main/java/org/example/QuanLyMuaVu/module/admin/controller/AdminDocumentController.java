package org.example.QuanLyMuaVu.module.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminDocumentResponse;
import org.example.QuanLyMuaVu.module.admin.service.AdminDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Document Management Controller
 * Provides CRUD operations for system documents (Admin only)
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {

    private final AdminDocumentService adminDocumentService;

    /**
     * GET /api/v1/admin/documents - List all documents with pagination
     */
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

    /**
     * GET /api/v1/admin/documents/{id} - Get document by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> getDocumentById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(adminDocumentService.getDocumentById(id)));
    }

    /**
     * POST /api/v1/admin/documents - Create new document
     */
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

    /**
     * PUT /api/v1/admin/documents/{id} - Update document
     */
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

    /**
     * DELETE /api/v1/admin/documents/{id} - Soft delete (set inactive)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDeleteDocument(@PathVariable Integer id) {
        adminDocumentService.softDeleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * DELETE /api/v1/admin/documents/{id}/permanent - Hard delete
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> hardDeleteDocument(@PathVariable Integer id) {
        adminDocumentService.hardDeleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Request record for create/update
    record CreateDocumentRequest(
            String title,
            String description,
            String documentUrl,
            String documentType,
            String status) {
    }
}
