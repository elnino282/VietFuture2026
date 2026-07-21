package org.example.season.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.season.dto.common.ApiResponse;
import org.example.season.dto.request.EmployeeTrainingRecordRequest;
import org.example.season.dto.request.TrainingProgramRequest;
import org.example.season.dto.response.EmployeeTrainingRecordDto;
import org.example.season.dto.response.TrainingProgramDto;
import org.example.season.service.EmployeeTrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/farmer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FARMER')")
public class EmployeeTrainingController {

    private final EmployeeTrainingService employeeTrainingService;

    @PostMapping("/training-programs")
    public ResponseEntity<ApiResponse<TrainingProgramDto>> createTrainingProgram(@RequestBody @Valid TrainingProgramRequest request) {
        TrainingProgramDto dto = employeeTrainingService.createTrainingProgram(request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/training-programs")
    public ResponseEntity<ApiResponse<List<TrainingProgramDto>>> getTrainingPrograms(@RequestParam(required = false) String category) {
        List<TrainingProgramDto> list = employeeTrainingService.getTrainingPrograms(category);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/employees/{userId}/training-records")
    public ResponseEntity<ApiResponse<EmployeeTrainingRecordDto>> recordTraining(
            @PathVariable Long userId,
            @RequestBody @Valid EmployeeTrainingRecordRequest request) {
        EmployeeTrainingRecordDto dto = employeeTrainingService.recordTraining(userId, request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/seasons/{seasonId}/training-status")
    public ResponseEntity<ApiResponse<Map<Long, List<EmployeeTrainingRecordDto>>>> getTrainingStatusForSeason(@PathVariable Integer seasonId) {
        Map<Long, List<EmployeeTrainingRecordDto>> status = employeeTrainingService.getTrainingStatusForSeason(seasonId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}

