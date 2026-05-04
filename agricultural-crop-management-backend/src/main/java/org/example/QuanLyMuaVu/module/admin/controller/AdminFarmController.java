package org.example.QuanLyMuaVu.module.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.admin.service.AdminFarmQueryService;
import org.example.QuanLyMuaVu.module.farm.dto.response.FarmResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Farm Controller
 * Provides global view of all farms for admin.
 */
@RestController
@RequestMapping("/api/v1/admin/farms")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminFarmController {

        private final AdminFarmQueryService adminFarmQueryService;

        /**
         * GET /api/v1/admin/farms
         * Returns paginated list of all farms using PageResponse format
         * that frontend expects (items instead of content).
         */
        @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<FarmResponse>>> getAllFarms(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String sortDir) {
                log.info("Admin requesting all farms, page: {}, size: {}", page, size);

                Sort sort = sortDir.equalsIgnoreCase("desc")
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<FarmResponse> farms = keyword != null && !keyword.isBlank()
                                ? adminFarmQueryService.searchFarms(keyword, pageable)
                                : adminFarmQueryService.getAllFarms(pageable);

                // Convert Page to PageResponse for consistent frontend format
                PageResponse<FarmResponse> pageResponse = PageResponse.of(farms, farms.getContent());

                return ResponseEntity.ok(ApiResponse.success("Farms retrieved", pageResponse));
        }

        /**
         * GET /api/v1/admin/farms/stats
         * Returns farm statistics
         */
        @GetMapping("/stats")
        public ResponseEntity<ApiResponse<FarmStats>> getFarmStats() {
                log.info("Admin requesting farm statistics");

                long activeFarms = adminFarmQueryService.countActiveFarms();
                long inactiveFarms = adminFarmQueryService.countInactiveFarms();

                FarmStats stats = new FarmStats(activeFarms, inactiveFarms, activeFarms + inactiveFarms);

                return ResponseEntity.ok(ApiResponse.success("Farm stats retrieved", stats));
        }

        public record FarmStats(long activeFarms, long inactiveFarms, long totalFarms) {
        }
}
