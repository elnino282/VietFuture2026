package org.example.QuanLyMuaVu.module.farm.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.farm.dto.request.FarmCreateRequest;
import org.example.QuanLyMuaVu.module.farm.dto.request.FarmUpdateRequest;
import org.example.QuanLyMuaVu.module.farm.dto.response.FarmResponse;
import org.example.QuanLyMuaVu.module.farm.service.FarmService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FARMER')")
public class FarmController {

        private final FarmService farmService;

        @GetMapping
        public ResponseEntity<ApiResponse<Object>> getMyFarms(
                        @RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "active", required = false) Boolean active,
                        @RequestParam(value = "page", required = false) Integer page,
                        @RequestParam(value = "size", required = false) Integer size) {
                boolean hasFilters = keyword != null || active != null || page != null || size != null;
                if (hasFilters) {
                        int pageValue = page != null ? page : 0;
                        int sizeValue = size != null ? size : 20;
                        PageResponse<FarmResponse> farms = farmService.searchMyFarms(keyword, active, pageValue,
                                        sizeValue);
                        return ResponseEntity.ok(ApiResponse.success(farms));
                }
                List<FarmResponse> farms = farmService.getMyFarms();
                return ResponseEntity.ok(ApiResponse.success(farms));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<FarmResponse>> createFarm(@RequestBody @Valid FarmCreateRequest request) {
                FarmResponse farm = farmService.createFarm(request);
                return ResponseEntity.ok(ApiResponse.success(farm));
        }

        @GetMapping("/{farmId}")
        public ResponseEntity<ApiResponse<FarmResponse>> getFarmDetail(@PathVariable Integer farmId) {
                FarmResponse farm = farmService.getFarmDetail(farmId);
                return ResponseEntity.ok(ApiResponse.success(farm));
        }

        @PutMapping("/{farmId}")
        public ResponseEntity<ApiResponse<FarmResponse>> updateFarm(
                        @PathVariable Integer farmId,
                        @RequestBody @Valid FarmUpdateRequest request) {
                FarmResponse farm = farmService.updateFarm(farmId, request);
                return ResponseEntity.ok(ApiResponse.success(farm));
        }

        @DeleteMapping("/{farmId}")
        public ResponseEntity<ApiResponse<String>> deleteFarm(@PathVariable Integer farmId) {
                farmService.deleteFarm(farmId);
                return ResponseEntity.ok(ApiResponse.success("Farm deleted successfully"));
        }
}
