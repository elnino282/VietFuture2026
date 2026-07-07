package org.example.farm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.farm.dto.common.ApiResponse;
import org.example.farm.dto.request.UpdateCertificationItemRequest;
import org.example.farm.dto.response.CertificationDetailsResponse;
import org.example.farm.service.CertificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/farms/{farmId}/certification")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FARMER')")
public class CertificationController {

    private final CertificationService certificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<CertificationDetailsResponse>> getCertificationDetails(@PathVariable Integer farmId) {
        CertificationDetailsResponse details = certificationService.getCertificationDetails(farmId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<String>> updateItemStatus(
            @PathVariable Integer farmId,
            @PathVariable Integer itemId,
            @RequestBody @Valid UpdateCertificationItemRequest request) {
        certificationService.updateItemStatus(farmId, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Item status updated successfully"));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<String>> applyCertification(@PathVariable Integer farmId) {
        try {
            certificationService.apply(farmId);
            return ResponseEntity.ok(ApiResponse.success("Đã nộp đơn xin chứng nhận VietGAP thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(org.springframework.http.HttpStatus.BAD_REQUEST, "ERR_BAD_REQUEST", e.getMessage()));
        }
    }
}
