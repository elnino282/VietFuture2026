package org.example.adminreporting.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.TaskResponse;
import org.example.adminreporting.entity.SeasonSummary;
import org.example.adminreporting.entity.TaskSummary;
import org.example.adminreporting.repository.SeasonSummaryRepository;
import org.example.adminreporting.repository.TaskSummaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminTaskController {

    private final TaskSummaryRepository taskSummaryRepository;
    private final SeasonSummaryRepository seasonSummaryRepository;
    private final org.example.adminreporting.mapper.AdminReportingMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> listAllTasks(
            @RequestParam(value = "farmId", required = false) Integer farmId,
            @RequestParam(value = "cropId", required = false) Integer cropId,
            @RequestParam(value = "seasonId", required = false) Integer seasonId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("Admin requesting all tasks from reporting service, farmId={}, cropId={}, seasonId={}, status={}",
                farmId, cropId, seasonId, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("taskId").descending());
        Page<TaskSummary> taskPage = taskSummaryRepository.findTasksWithFilters(farmId, cropId, seasonId, status, pageable);

        // Pre-fetch season names to avoid N+1 queries
        List<Integer> seasonIds = taskPage.getContent().stream().map(TaskSummary::getSeasonId).distinct().toList();
        Map<Integer, String> seasonNameMap = seasonSummaryRepository.findAllById(seasonIds).stream()
                .collect(Collectors.toMap(SeasonSummary::getSeasonId, SeasonSummary::getSeasonName));

        Page<TaskResponse> responsePage = taskPage.map(task -> mapper.toTaskResponse(task, seasonNameMap.get(task.getSeasonId())));
        PageResponse<TaskResponse> pageResponse = PageResponse.of(responsePage, responsePage.getContent());

        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved", pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Integer id) {
        log.info("Admin requesting task detail for task ID: {}", id);
        TaskSummary task = taskSummaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));

        String seasonName = seasonSummaryRepository.findById(task.getSeasonId())
                .map(SeasonSummary::getSeasonName)
                .orElse("Season " + task.getSeasonId());

        return ResponseEntity.ok(ApiResponse.success("Task retrieved", mapper.toTaskResponse(task, seasonName)));
    }

}
