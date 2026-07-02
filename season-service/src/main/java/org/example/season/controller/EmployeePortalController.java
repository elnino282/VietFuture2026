package org.example.season.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.dto.common.ApiResponse;
import org.example.season.dto.common.PageResponse;
import org.example.season.dto.request.EmployeeTaskProgressRequest;
import org.example.season.dto.response.MySeasonResponse;
import org.example.season.dto.response.PayrollRecordResponse;
import org.example.season.dto.response.TaskProgressLogResponse;
import org.example.season.dto.response.TaskResponse;
import org.example.season.entity.Season;
import org.example.season.service.LaborManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeePortalController {

    LaborManagementService laborManagementService;

    @GetMapping("/seasons")
    public ApiResponse<List<MySeasonResponse>> getMySeasons() {
        return ApiResponse.success(laborManagementService.listAssignedSeasonsForCurrentEmployee());
    }

    @GetMapping("/seasons/{seasonId}")
    public ApiResponse<Season> getMySeasonDetail(@PathVariable Integer seasonId) {
        return ApiResponse.success(laborManagementService.getAssignedSeasonForCurrentEmployee(seasonId));
    }

    @GetMapping("/tasks")
    public ApiResponse<PageResponse<TaskResponse>> getMyTasks(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(laborManagementService.listAssignedTasksForCurrentEmployee(status, seasonId, page, size));
    }

    @GetMapping("/seasons/{seasonId}/plan")
    public ApiResponse<List<TaskResponse>> getMySeasonPlan(@PathVariable Integer seasonId) {
        return ApiResponse.success(laborManagementService.getSeasonPlanForCurrentEmployee(seasonId));
    }

    @PatchMapping("/tasks/{taskId}/accept")
    public ApiResponse<TaskResponse> acceptTask(@PathVariable Integer taskId) {
        return ApiResponse.success(laborManagementService.acceptTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/progress")
    public ApiResponse<TaskProgressLogResponse> reportTaskProgress(
            @PathVariable Integer taskId,
            @Valid @RequestBody EmployeeTaskProgressRequest request) {
        return ApiResponse.success(laborManagementService.reportTaskProgress(taskId, request));
    }

    @GetMapping("/progress")
    public ApiResponse<PageResponse<TaskProgressLogResponse>> getMyProgress(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(laborManagementService.listMyProgress(page, size));
    }

    @GetMapping("/payroll")
    public ApiResponse<PageResponse<PayrollRecordResponse>> getMyPayroll(
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(laborManagementService.listMyPayroll(seasonId, page, size));
    }

    @GetMapping("/payroll/{payrollRecordId}")
    public ApiResponse<PayrollRecordResponse> getMyPayrollDetail(@PathVariable Integer payrollRecordId) {
        return ApiResponse.success(laborManagementService.getMyPayrollDetail(payrollRecordId));
    }
}
