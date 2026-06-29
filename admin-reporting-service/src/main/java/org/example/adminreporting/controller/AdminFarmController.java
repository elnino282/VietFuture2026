package org.example.adminreporting.controller;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.FarmResponse;
import org.example.adminreporting.entity.FarmSummary;
import org.example.adminreporting.repository.FarmSummaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/farms")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminFarmController {

    private final FarmSummaryRepository farmSummaryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FarmResponse>>> getAllFarms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "farmId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Admin requesting all farms from reporting service, page: {}, size: {}", page, size);

        // Map sortBy field to entity field if needed
        String entitySortBy = sortBy;
        if ("id".equals(sortBy)) {
            entitySortBy = "farmId";
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(entitySortBy).descending()
                : Sort.by(entitySortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FarmSummary> farmPage = keyword != null && !keyword.isBlank()
                ? farmSummaryRepository.findByFarmNameContainingIgnoreCase(keyword, pageable)
                : farmSummaryRepository.findAll(pageable);

        Page<FarmResponse> responsePage = farmPage.map(this::toFarmResponse);
        PageResponse<FarmResponse> pageResponse = PageResponse.of(responsePage, responsePage.getContent());

        return ResponseEntity.ok(ApiResponse.success("Farms retrieved", pageResponse));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<FarmStats>> getFarmStats() {
        log.info("Admin requesting farm statistics from reporting service");

        long activeFarms = farmSummaryRepository.countActiveFarms();
        long inactiveFarms = farmSummaryRepository.countInactiveFarms();

        FarmStats stats = new FarmStats(activeFarms, inactiveFarms, activeFarms + inactiveFarms);

        return ResponseEntity.ok(ApiResponse.success("Farm stats retrieved", stats));
    }

    private FarmResponse toFarmResponse(FarmSummary summary) {
        return FarmResponse.builder()
                .id(summary.getFarmId())
                .farmName(summary.getFarmName())
                .name(summary.getFarmName())
                .active(summary.getActive())
                .build();
    }

    public record FarmStats(long activeFarms, long inactiveFarms, long totalFarms) {
    }
}
