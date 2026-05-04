package org.example.QuanLyMuaVu.module.sustainability.controller;



import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.LowStockAlertResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.PlotStatusResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.TodayTaskResponse;
import org.example.QuanLyMuaVu.module.sustainability.service.DashboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard Controller
 * Farmer Portal dashboard aggregation endpoints.
 * All endpoints are JWT protected and return only owner-scoped data.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('FARMER')")
public class DashboardController {

        private final DashboardService dashboardService;

        /**
         * GET /api/v1/dashboard/overview
         * Returns all aggregated metrics for the dashboard.
         * 
         * @param seasonId Optional season ID to scope metrics. If null, defaults to
         *                 most recent ACTIVE season.
         */
        @GetMapping("/overview")
        public ApiResponse<DashboardOverviewResponse> getOverview(
                        @RequestParam(required = false) Integer seasonId) {
                log.debug("GET /dashboard/overview?seasonId={}", seasonId);
                DashboardOverviewResponse response = dashboardService.getOverview(seasonId);
                return ApiResponse.success(response);
        }

        /**
         * GET /api/v1/dashboard/today-tasks
         * Returns paginated list of today's tasks.
         */
        @GetMapping("/today-tasks")
        public ApiResponse<Page<TodayTaskResponse>> getTodayTasks(
                        @RequestParam(required = false) Integer seasonId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "dueDate,asc") String sort) {
                log.debug("GET /dashboard/today-tasks?seasonId={}&page={}&size={}", seasonId, page, size);

                String[] sortParts = sort.split(",");
                Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

                Page<TodayTaskResponse> tasks = dashboardService.getTodayTasks(seasonId, pageable);
                return ApiResponse.success(tasks);
        }

        /**
         * GET /api/v1/dashboard/upcoming-tasks
         * Returns upcoming tasks within specified days.
         */
        @GetMapping("/upcoming-tasks")
        public ApiResponse<List<TodayTaskResponse>> getUpcomingTasks(
                        @RequestParam(defaultValue = "7") int days,
                        @RequestParam(required = false) Integer seasonId) {
                log.debug("GET /dashboard/upcoming-tasks?days={}&seasonId={}", days, seasonId);
                List<TodayTaskResponse> tasks = dashboardService.getUpcomingTasks(days, seasonId);
                return ApiResponse.success(tasks);
        }

        /**
         * GET /api/v1/dashboard/plot-status
         * Returns plot status list for the Plot Status Map panel.
         */
        @GetMapping("/plot-status")
        public ApiResponse<List<PlotStatusResponse>> getPlotStatus(
                        @RequestParam(required = false) Integer seasonId) {
                log.debug("GET /dashboard/plot-status?seasonId={}", seasonId);
                List<PlotStatusResponse> plots = dashboardService.getPlotStatus(seasonId);
                return ApiResponse.success(plots);
        }

        /**
         * GET /api/v1/dashboard/low-stock
         * Returns low stock alert items.
         */
        @GetMapping("/low-stock")
        public ApiResponse<List<LowStockAlertResponse>> getLowStock(
                        @RequestParam(defaultValue = "5") int limit) {
                log.debug("GET /dashboard/low-stock?limit={}", limit);
                List<LowStockAlertResponse> items = dashboardService.getLowStock(limit);
                return ApiResponse.success(items);
        }
}
