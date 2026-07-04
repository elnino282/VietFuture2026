package org.example.season.service;

import lombok.RequiredArgsConstructor;
import org.example.season.client.FarmServiceClient;
import org.example.season.client.FarmServiceClient.PlotInternalDto;
import org.example.season.dto.TaskDetailDto;
import org.example.season.entity.Task;
import org.example.season.repository.TaskRepository;
import org.example.season.repository.WorkTeamMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskAssignmentService {

    private final TaskRepository taskRepository;
    private final WorkTeamMemberRepository workTeamMemberRepository;
    private final FarmServiceClient farmServiceClient;

    @Transactional
    public Task assignTaskToTeamAndPlot(Integer taskId, Long workTeamId, Long plotId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
                
        task.setWorkTeamId(workTeamId);
        task.setPlotId(plotId);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDetailDto> getTasksForEmployee(Long employeeUserId) {
        List<Long> teamIds = workTeamMemberRepository.findByEmployeeUserId(employeeUserId)
                .stream()
                .map(member -> member.getWorkTeam().getId())
                .collect(Collectors.toList());

        if (teamIds.isEmpty()) teamIds.add(-1L);

        List<Task> tasks = taskRepository.findTasksForEmployee(employeeUserId, teamIds);

        List<Long> distinctPlotIds = tasks.stream()
                .map(Task::getPlotId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // [FIX] - Refactored to Stream API (Optional) to make plotCache effectively final
        final Map<Long, PlotInternalDto> plotCache = java.util.Optional.of(distinctPlotIds)
                .filter(ids -> !ids.isEmpty())
                .map(ids -> {
                    try {
                        return farmServiceClient.getBulkPlots(ids);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElseGet(HashMap::new);

        return tasks.stream().map(task -> {
            PlotInternalDto plot = task.getPlotId() != null ? plotCache.get(task.getPlotId()) : null;
            return TaskDetailDto.builder()
                .taskId(Long.valueOf(task.getId()))
                .taskName(task.getTitle())
                .estimatedDays(task.getEstimatedDays()) 
                .estimatedCompletionDate(task.getEstimatedCompletionDate())
                .status(task.getStatus())
                .plotId(task.getPlotId())
                .plotName(plot != null ? plot.getPlotName() : null)
                .plotArea(plot != null ? plot.getPlotArea() : null)
                .workTeamId(task.getWorkTeamId())
                .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<org.example.season.dto.TeamProgressSummaryResponse> getTeamProgressSummary(Long seasonId) {
        return taskRepository.getTeamProgressSummary(seasonId);
    }
}
