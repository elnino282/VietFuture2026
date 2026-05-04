package org.example.QuanLyMuaVu.module.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.admin.dto.response.DocumentResponse;
import org.example.QuanLyMuaVu.module.admin.service.DocumentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only access for farmers to view documents, manage favorites, and track
 * recently opened.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    /**
     * List documents with filters and tab support
     * GET
     * /api/v1/documents?tab=all|favorites|recent&q=&crop=&stage=&topic=&page=&size=
     */
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping
    public ApiResponse<PageResponse<DocumentResponse>> list(
            @RequestParam(defaultValue = "all") String tab,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = currentUserService.getCurrentUserId();
        PageResponse<DocumentResponse> result = documentService.listDocuments(
                tab, q, crop, stage, topic, page, size, userId);
        return ApiResponse.success(result);
    }

    /**
     * Get single document by ID
     * GET /api/v1/documents/{id}
     */
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> getById(@PathVariable Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        DocumentResponse doc = documentService.getById(id, userId);
        return ApiResponse.success(doc);
    }

    /**
     * Record document open (for Recent tab)
     * POST /api/v1/documents/{id}/open
     */
    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/{id}/open")
    public ApiResponse<Void> recordOpen(@PathVariable Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        documentService.recordOpen(id, userId);
        return ApiResponse.success(null);
    }

    /**
     * Add document to favorites
     * POST /api/v1/documents/{id}/favorite
     */
    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/{id}/favorite")
    public ApiResponse<Void> addFavorite(@PathVariable Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        documentService.addFavorite(id, userId);
        return ApiResponse.success(null);
    }

    /**
     * Remove document from favorites
     * DELETE /api/v1/documents/{id}/favorite
     */
    @PreAuthorize("hasRole('FARMER')")
    @DeleteMapping("/{id}/favorite")
    public ApiResponse<Void> removeFavorite(@PathVariable Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        documentService.removeFavorite(id, userId);
        return ApiResponse.success(null);
    }
}
