package org.example.QuanLyMuaVu.module.sustainability.controller;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminReportResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.FarmerReportFilter;
import org.example.QuanLyMuaVu.module.sustainability.service.FarmerReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farmer/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FARMER')")
public class FarmerReportController {

    private final FarmerReportService farmerReportService;

    private FarmerReportFilter buildFilter(
            Integer seasonId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Integer cropId,
            Integer farmId,
            Integer plotId,
            Integer page,
            Integer size) {
        return FarmerReportFilter.builder()
                .seasonId(seasonId)
                .fromDate(dateFrom)
                .toDate(dateTo)
                .cropId(cropId)
                .farmId(farmId)
                .plotId(plotId)
                .page(page)
                .size(size)
                .build();
    }

    @GetMapping("/yield")
    public ApiResponse<List<AdminReportResponse.YieldReport>> getYieldReport(
            @RequestParam(required = false) Integer seasonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer cropId,
            @RequestParam(required = false) Integer farmId,
            @RequestParam(required = false) Integer plotId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        FarmerReportFilter filter = buildFilter(seasonId, dateFrom, dateTo, cropId, farmId, plotId, page, size);
        return ApiResponse.success("Yield report generated", farmerReportService.getYieldReport(filter));
    }

    @GetMapping("/cost")
    public ApiResponse<List<AdminReportResponse.CostReport>> getCostReport(
            @RequestParam(required = false) Integer seasonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer cropId,
            @RequestParam(required = false) Integer farmId,
            @RequestParam(required = false) Integer plotId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        FarmerReportFilter filter = buildFilter(seasonId, dateFrom, dateTo, cropId, farmId, plotId, page, size);
        return ApiResponse.success("Cost report generated", farmerReportService.getCostReport(filter));
    }

    @GetMapping("/revenue")
    public ApiResponse<List<AdminReportResponse.RevenueReport>> getRevenueReport(
            @RequestParam(required = false) Integer seasonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer cropId,
            @RequestParam(required = false) Integer farmId,
            @RequestParam(required = false) Integer plotId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        FarmerReportFilter filter = buildFilter(seasonId, dateFrom, dateTo, cropId, farmId, plotId, page, size);
        return ApiResponse.success("Revenue report generated", farmerReportService.getRevenueReport(filter));
    }

    @GetMapping("/profit")
    public ApiResponse<List<AdminReportResponse.ProfitReport>> getProfitReport(
            @RequestParam(required = false) Integer seasonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Integer cropId,
            @RequestParam(required = false) Integer farmId,
            @RequestParam(required = false) Integer plotId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        FarmerReportFilter filter = buildFilter(seasonId, dateFrom, dateTo, cropId, farmId, plotId, page, size);
        return ApiResponse.success("Profit report generated", farmerReportService.getProfitReport(filter));
    }
}
