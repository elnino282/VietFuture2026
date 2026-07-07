package org.example.farm.controller;

import lombok.RequiredArgsConstructor;
import org.example.farm.dto.common.ApiResponse;
import org.example.farm.dto.request.FarmDocumentCreateRequest;
import org.example.farm.dto.response.FarmDocumentResponse;
import org.example.farm.service.FarmDocumentService;
import org.example.farm.service.FarmStorageService;
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

    @PostMapping
    public ResponseEntity<ApiResponse<FarmDocumentResponse>> create(
            @PathVariable Integer farmId,
            @RequestBody FarmDocumentCreateRequest req,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(service.create(farmId, userId, req)));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> upload(
            @PathVariable Integer farmId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(storageService.storeDocument(file, userId)));
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
