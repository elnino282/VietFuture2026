package org.example.QuanLyMuaVu.module.season.controller;


import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.season.dto.request.AddSeasonEmployeeRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.AssignTaskEmployeeRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.BulkAssignSeasonEmployeesRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.PayrollRecalculateRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdatePayrollRecordRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonEmployeeRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.EmployeeDirectoryResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.PayrollRecordResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonEmployeeResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskProgressLogResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.service.LaborManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/farmer/labor")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class FarmerLaborManagementController {

    LaborManagementService laborManagementService;

    @GetMapping("/employees/directory")
    public ApiResponse<PageResponse<EmployeeDirectoryResponse>> listEmployeeDirectory(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(laborManagementService.listEmployeeDirectory(keyword, page, size));
    }

    @GetMapping("/seasons/{seasonId}/employees")
    public ApiResponse<PageResponse<SeasonEmployeeResponse>> listSeasonEmployees(
            @PathVariable Integer seasonId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(laborManagementService.listSeasonEmployees(seasonId, keyword, page, size));
    }

    @PostMapping("/seasons/{seasonId}/employees")
    public ApiResponse<SeasonEmployeeResponse> addSeasonEmployee(
            @PathVariable Integer seasonId,
            @Valid @RequestBody AddSeasonEmployeeRequest request) {
        return ApiResponse.success(laborManagementService.addSeasonEmployee(seasonId, request));
    }

    @PostMapping("/seasons/{seasonId}/employees/bulk")
    public ApiResponse<List<SeasonEmployeeResponse>> bulkAssignSeasonEmployees(
            @PathVariable Integer seasonId,
            @Valid @RequestBody BulkAssignSeasonEmployeesRequest request) {
        return ApiResponse.success(laborManagementService.bulkAssignSeasonEmployees(seasonId, request));
    }

    @PatchMapping("/seasons/{seasonId}/employees/{employeeUserId}")
    public ApiResponse<SeasonEmployeeResponse> updateSeasonEmployee(
            @PathVariable Integer seasonId,
            @PathVariable Long employeeUserId,
            @Valid @RequestBody UpdateSeasonEmployeeRequest request) {
        return ApiResponse.success(laborManagementService.updateSeasonEmployee(seasonId, employeeUserId, request));
    }

    @DeleteMapping("/seasons/{seasonId}/employees/{employeeUserId}")
    public ApiResponse<Void> removeSeasonEmployee(
            @PathVariable Integer seasonId,
            @PathVariable Long employeeUserId) {
        laborManagementService.removeSeasonEmployee(seasonId, employeeUserId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/tasks/{taskId}/assign")
    public ApiResponse<TaskResponse> assignTaskToEmployee(
            @PathVariable Integer taskId,
            @Valid @RequestBody AssignTaskEmployeeRequest request) {
        return ApiResponse.success(laborManagementService.assignTaskToEmployee(taskId, request.getEmployeeUserId()));
    }

    @GetMapping("/seasons/{seasonId}/progress")
    public ApiResponse<PageResponse<TaskProgressLogResponse>> listSeasonProgress(
            @PathVariable Integer seasonId,
            @RequestParam(value = "employeeUserId", required = false) Long employeeUserId,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(
                laborManagementService.listSeasonProgress(seasonId, employeeUserId, taskId, page, size));
    }

    @GetMapping("/seasons/{seasonId}/payroll")
    public ApiResponse<PageResponse<PayrollRecordResponse>> listSeasonPayroll(
            @PathVariable Integer seasonId,
            @RequestParam(value = "employeeUserId", required = false) Long employeeUserId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(laborManagementService.listSeasonPayroll(seasonId, employeeUserId, page, size));
    }

    @GetMapping("/seasons/{seasonId}/payroll/{payrollRecordId}")
    public ApiResponse<PayrollRecordResponse> getSeasonPayrollDetail(
            @PathVariable Integer seasonId,
            @PathVariable Integer payrollRecordId) {
        return ApiResponse.success(laborManagementService.getSeasonPayrollDetail(seasonId, payrollRecordId));
    }

    @PatchMapping("/seasons/{seasonId}/payroll/{payrollRecordId}")
    public ApiResponse<PayrollRecordResponse> updateSeasonPayroll(
            @PathVariable Integer seasonId,
            @PathVariable Integer payrollRecordId,
            @Valid @RequestBody UpdatePayrollRecordRequest request) {
        return ApiResponse.success(laborManagementService.updateSeasonPayroll(seasonId, payrollRecordId, request));
    }

    @PostMapping("/seasons/{seasonId}/payroll/recalculate")
    public ApiResponse<List<PayrollRecordResponse>> recalculatePayroll(
            @PathVariable Integer seasonId,
            @RequestBody(required = false) PayrollRecalculateRequest request) {
        PayrollRecalculateRequest payload = request != null ? request : new PayrollRecalculateRequest();
        return ApiResponse.success(laborManagementService.recalculatePayroll(
                seasonId,
                payload.getEmployeeUserId(),
                payload.getPeriodStart(),
                payload.getPeriodEnd()));
    }
}
