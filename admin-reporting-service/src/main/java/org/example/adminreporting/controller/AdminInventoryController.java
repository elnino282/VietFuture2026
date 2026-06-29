package org.example.adminreporting.controller;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.AdminInventoryLotDetailResponse;
import org.example.adminreporting.dto.response.AdminInventoryMovementResponse;
import org.example.adminreporting.dto.response.AdminInventoryOptionsResponse;
import org.example.adminreporting.dto.response.AdminInventoryRiskLotResponse;
import org.example.adminreporting.service.AdminInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {

    private final AdminInventoryService adminInventoryService;

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<AdminInventoryOptionsResponse>> getOptions() {
        log.info("Admin requesting inventory options from reporting service");
        return ResponseEntity.ok(ApiResponse.success("Options retrieved", adminInventoryService.getOptions()));
    }

    @GetMapping("/lots")
    public ResponseEntity<ApiResponse<PageResponse<AdminInventoryRiskLotResponse>>> listRiskLots(
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "windowDays", required = false) Integer windowDays,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "lowStockThreshold", required = false) BigDecimal lowStockThreshold,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        log.info("Admin requesting risk lots from reporting service, farmId={}, status={}", farmId, status);
        return ResponseEntity.ok(ApiResponse.success("Lots retrieved", adminInventoryService.listRiskLots(
                farmId, itemId, status, severity, windowDays, q, sort, lowStockThreshold, page, limit)));
    }

    @GetMapping("/lots/{lotId}")
    public ResponseEntity<ApiResponse<AdminInventoryLotDetailResponse>> getLotDetail(@PathVariable("lotId") Integer lotId) {
        log.info("Admin requesting lot detail for lot ID: {}", lotId);
        return ResponseEntity.ok(ApiResponse.success("Lot detail retrieved", adminInventoryService.getLotDetail(lotId)));
    }

    @GetMapping("/lots/{lotId}/movements")
    public ResponseEntity<ApiResponse<List<AdminInventoryMovementResponse>>> getLotMovements(@PathVariable("lotId") Integer lotId) {
        log.info("Admin requesting movements for lot ID: {}", lotId);
        return ResponseEntity.ok(ApiResponse.success("Lot movements retrieved", adminInventoryService.getLotMovements(lotId)));
    }
}
