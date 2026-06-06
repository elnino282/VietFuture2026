package org.example.QuanLyMuaVu.module.season.controller;


import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.ai.dto.request.DiseaseSuggestionRequest;
import org.example.QuanLyMuaVu.module.ai.dto.response.DiseaseSuggestionResponse;
import org.example.QuanLyMuaVu.module.ai.service.DiseaseSuggestionService;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplyItemResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplyLotResponse;
import org.example.QuanLyMuaVu.module.inventory.service.SuppliesService;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateDiseaseRecordRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateDiseaseTreatmentRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateFieldLogRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.EmployeeTaskProgressRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateDiseaseRecordRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateDiseaseTreatmentRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateFieldLogRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseRecordDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseRecordResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseTreatmentResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.FieldLogResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.MySeasonResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.PayrollRecordResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskProgressLogResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.service.DiseaseRecordService;
import org.example.QuanLyMuaVu.module.season.service.FieldLogService;
import org.example.QuanLyMuaVu.module.season.service.LaborManagementService;
import org.example.QuanLyMuaVu.module.season.service.SeasonWorkspaceAccessService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    FieldLogService fieldLogService;
    DiseaseRecordService diseaseRecordService;
    DiseaseSuggestionService diseaseSuggestionService;
    SuppliesService suppliesService;
    SeasonWorkspaceAccessService seasonWorkspaceAccessService;

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

    @GetMapping("/seasons")
    public ApiResponse<List<MySeasonResponse>> listAssignedSeasons() {
        return ApiResponse.success(laborManagementService.listAssignedSeasonsForCurrentEmployee());
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

    @GetMapping("/seasons/{seasonId}/field-logs")
    public ApiResponse<PageResponse<FieldLogResponse>> listFieldLogs(
            @PathVariable Integer seasonId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(
                fieldLogService.listFieldLogsForAssignedEmployeeSeason(seasonId, from, to, type, q, page, size));
    }

    @PostMapping("/seasons/{seasonId}/field-logs")
    public ApiResponse<FieldLogResponse> createFieldLog(
            @PathVariable Integer seasonId,
            @Valid @RequestBody CreateFieldLogRequest request) {
        return ApiResponse.success(fieldLogService.createFieldLogForAssignedEmployee(seasonId, request));
    }

    @GetMapping("/field-logs/{id}")
    public ApiResponse<FieldLogResponse> getFieldLog(@PathVariable Integer id) {
        return ApiResponse.success(fieldLogService.getFieldLogForAssignedEmployee(id));
    }

    @PutMapping("/field-logs/{id}")
    public ApiResponse<FieldLogResponse> updateFieldLog(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateFieldLogRequest request) {
        return ApiResponse.success(fieldLogService.updateFieldLogForAssignedEmployee(id, request));
    }

    @DeleteMapping("/field-logs/{id}")
    public ApiResponse<Void> deleteFieldLog(@PathVariable Integer id) {
        fieldLogService.deleteFieldLogForAssignedEmployee(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/seasons/{seasonId}/disease-records")
    public ApiResponse<PageResponse<DiseaseRecordResponse>> listDiseaseRecords(
            @PathVariable Integer seasonId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "fromDetectedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDetectedAt,
            @RequestParam(value = "toDetectedAt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDetectedAt,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(diseaseRecordService.listDiseaseRecordsByAssignedEmployeeSeason(
                seasonId,
                status,
                severity,
                q,
                fromDetectedAt,
                toDetectedAt,
                page,
                size));
    }

    @PostMapping("/seasons/{seasonId}/disease-records")
    public ApiResponse<DiseaseRecordResponse> createDiseaseRecord(
            @PathVariable Integer seasonId,
            @Valid @RequestBody CreateDiseaseRecordRequest request) {
        return ApiResponse.success(diseaseRecordService.createDiseaseRecordForAssignedEmployee(seasonId, request));
    }

    @GetMapping("/disease-records/{id}")
    public ApiResponse<DiseaseRecordDetailResponse> getDiseaseRecordDetail(@PathVariable Integer id) {
        return ApiResponse.success(diseaseRecordService.getDiseaseRecordDetailForAssignedEmployee(id));
    }

    @PutMapping("/disease-records/{id}")
    public ApiResponse<DiseaseRecordResponse> updateDiseaseRecord(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateDiseaseRecordRequest request) {
        return ApiResponse.success(diseaseRecordService.updateDiseaseRecordForAssignedEmployee(id, request));
    }

    @DeleteMapping("/disease-records/{id}")
    public ApiResponse<Void> deleteDiseaseRecord(@PathVariable Integer id) {
        diseaseRecordService.deleteDiseaseRecordForAssignedEmployee(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/disease-records/{id}/treatments")
    public ApiResponse<PageResponse<DiseaseTreatmentResponse>> listTreatments(
            @PathVariable Integer id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(diseaseRecordService.listTreatmentsForAssignedEmployee(id, page, size));
    }

    @PostMapping("/disease-records/{id}/treatments")
    public ApiResponse<DiseaseTreatmentResponse> createTreatment(
            @PathVariable Integer id,
            @Valid @RequestBody CreateDiseaseTreatmentRequest request) {
        return ApiResponse.success(diseaseRecordService.createTreatmentForAssignedEmployee(id, request));
    }

    @PutMapping("/disease-treatments/{id}")
    public ApiResponse<DiseaseTreatmentResponse> updateTreatment(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateDiseaseTreatmentRequest request) {
        return ApiResponse.success(diseaseRecordService.updateTreatmentForAssignedEmployee(id, request));
    }

    @DeleteMapping("/disease-treatments/{id}")
    public ApiResponse<Void> deleteTreatment(@PathVariable Integer id) {
        diseaseRecordService.deleteTreatmentForAssignedEmployee(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/disease-records/{id}/ai-suggestion")
    public ApiResponse<DiseaseSuggestionResponse> generateAiSuggestion(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) DiseaseSuggestionRequest request) {
        return ApiResponse.success(diseaseSuggestionService.generateSuggestion(id, request));
    }

    @GetMapping("/seasons/{seasonId}/supplies/items")
    public ApiResponse<PageResponse<SupplyItemResponse>> listSeasonSupplyItems(
            @PathVariable Integer seasonId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "restricted", required = false) Boolean restricted,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Season season = laborManagementService.getAssignedSeasonForCurrentEmployee(seasonId);
        Integer farmId = seasonWorkspaceAccessService.resolveSeasonFarmId(season);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(suppliesService.getSupplyItemsForFarmIds(
                farmId != null ? List.of(farmId) : List.of(),
                q,
                restricted,
                pageable));
    }

    @GetMapping("/seasons/{seasonId}/supplies/lots")
    public ApiResponse<PageResponse<SupplyLotResponse>> listSeasonSupplyLots(
            @PathVariable Integer seasonId,
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Season season = laborManagementService.getAssignedSeasonForCurrentEmployee(seasonId);
        Integer farmId = seasonWorkspaceAccessService.resolveSeasonFarmId(season);
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(suppliesService.getSupplyLotsForFarmIds(
                farmId != null ? List.of(farmId) : List.of(),
                itemId,
                status,
                q,
                pageable));
    }
}
