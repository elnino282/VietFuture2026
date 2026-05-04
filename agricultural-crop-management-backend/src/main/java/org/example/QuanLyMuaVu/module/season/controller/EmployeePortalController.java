package org.example.QuanLyMuaVu.module.season.controller;


import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.season.dto.request.EmployeeTaskProgressRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.PayrollRecordResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskProgressLogResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.service.LaborManagementService;
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

    @GetMapping("/tasks")
    public ApiResponse<PageResponse<TaskResponse>> listAssignedTasks(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(laborManagementService.listAssignedTasksForCurrentEmployee(status, seasonId, page, size));
    }

    @GetMapping("/seasons/{seasonId}/plan")
    public ApiResponse<List<TaskResponse>> getSeasonPlan(@PathVariable Integer seasonId) {
        return ApiResponse.success(laborManagementService.getSeasonPlanForCurrentEmployee(seasonId));
    }

    @PatchMapping("/tasks/{taskId}/accept")
    public ApiResponse<TaskResponse> acceptTask(@PathVariable Integer taskId) {
        return ApiResponse.success(laborManagementService.acceptTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/progress")
    public ApiResponse<TaskProgressLogResponse> reportProgress(
            @PathVariable Integer taskId,
            @Valid @RequestBody EmployeeTaskProgressRequest request) {
        return ApiResponse.success(laborManagementService.reportTaskProgress(taskId, request));
    }

    @GetMapping("/progress")
    public ApiResponse<PageResponse<TaskProgressLogResponse>> listMyProgress(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(laborManagementService.listMyProgress(page, size));
    }

    @GetMapping("/payroll")
    public ApiResponse<PageResponse<PayrollRecordResponse>> listMyPayroll(
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
