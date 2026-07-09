package org.example.farm.controller;

import lombok.RequiredArgsConstructor;
import org.example.farm.dto.common.ApiResponse;
import org.example.farm.dto.request.FarmDocumentCreateRequest;
import org.example.farm.dto.response.FarmDocumentResponse;
import org.example.farm.service.FarmDocumentService;
import org.example.farm.service.FarmStorageService;
import org.example.farm.config.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/farms/{farmId}/documents")
@RequiredArgsConstructor
public class FarmDocumentController {

    private final FarmDocumentService service;
    private final FarmStorageService storageService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<FarmDocumentResponse>> create(
            @PathVariable Integer farmId,
            @RequestBody FarmDocumentCreateRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Long resolvedUserId = userId != null ? userId : currentUserService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.create(farmId, resolvedUserId, req)));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> upload(
            @PathVariable Integer farmId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Long resolvedUserId = userId != null ? userId : currentUserService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(storageService.storeDocument(file, resolvedUserId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FarmDocumentResponse>>> list(
            @PathVariable Integer farmId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByFarmId(farmId)));
    }

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<FarmDocumentResponse>>> expiring(
            @PathVariable Integer farmId) {
        return ResponseEntity.ok(ApiResponse.success(service.getExpiringDocuments(farmId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer farmId,
            @PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
