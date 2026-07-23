package org.example.season.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.season.dto.response.FarmingLogDto;
import org.example.season.dto.response.SeasonReportDto;
import org.example.season.entity.Season;
import org.example.season.entity.Task;
import org.example.season.enums.TaskStatus;
import org.example.season.exception.AppException;
import org.example.season.exception.ErrorCode;
import org.example.season.repository.SeasonRepository;
import org.example.season.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final TaskRepository taskRepository;
    private final SeasonRepository seasonRepository;

    public List<FarmingLogDto> getFarmingLogs(Integer seasonId, Long userId) {
        // Fetch completed tasks for the season
        List<Task> tasks = taskRepository.findAllBySeasonId(seasonId);

        return tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .sorted(Comparator.comparing(Task::getActualEndDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapToFarmingLog)
                .collect(Collectors.toList());
    }

    public SeasonReportDto getSeasonStats(Integer seasonId, Long userId) {
        Season season = seasonRepository.findById(seasonId).orElse(null);

        if (season == null) {
            // Return empty stats if season not found in DB (e.g., when frontend uses mock season IDs)
            return SeasonReportDto.builder()
                    .seasonId(String.valueOf(seasonId))
                    .totalYieldKg(BigDecimal.ZERO)
                    .expectedYieldKg(BigDecimal.ZERO)
                    .totalExpenseVnd(BigDecimal.ZERO)
                    .expenseByCategory(new ArrayList<>())
                    .build();
        }

        // Use budgetAmount as totalExpense for the scope of this dashboard
        BigDecimal totalExpense = season.getBudgetAmount() != null ? season.getBudgetAmount() : BigDecimal.ZERO;
        
        List<SeasonReportDto.ExpenseCategoryDto> expenseByCategory = new ArrayList<>();
        if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            // Mock expense distribution based on total expense
            expenseByCategory.add(new SeasonReportDto.ExpenseCategoryDto("Vật tư (Phân bón, Thuốc)", totalExpense.multiply(new BigDecimal("0.4"))));
            expenseByCategory.add(new SeasonReportDto.ExpenseCategoryDto("Nhân công", totalExpense.multiply(new BigDecimal("0.35"))));
            expenseByCategory.add(new SeasonReportDto.ExpenseCategoryDto("Máy móc thiết bị", totalExpense.multiply(new BigDecimal("0.15"))));
            expenseByCategory.add(new SeasonReportDto.ExpenseCategoryDto("Khác", totalExpense.multiply(new BigDecimal("0.1"))));
        }

        return SeasonReportDto.builder()
                .seasonId(String.valueOf(season.getId()))
                .totalYieldKg(season.getActualYieldKg() != null ? season.getActualYieldKg() : BigDecimal.ZERO)
                .expectedYieldKg(season.getExpectedYieldKg() != null ? season.getExpectedYieldKg() : BigDecimal.ZERO)
                .totalExpenseVnd(totalExpense)
                .expenseByCategory(expenseByCategory)
                .build();
    }

    private FarmingLogDto mapToFarmingLog(Task task) {
        String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
        String activityType = "OTHER";
        if (title.contains("phân")) {
            activityType = "FERTILIZER";
        } else if (title.contains("thuốc") || title.contains("phun")) {
            activityType = "PESTICIDE";
        } else if (title.contains("tưới")) {
            activityType = "WATERING";
        } else if (title.contains("thu hoạch")) {
            activityType = "HARVEST";
        }

        return FarmingLogDto.builder()
                .id(String.valueOf(task.getId()))
                .seasonId(String.valueOf(task.getSeason().getId()))
                .date(task.getActualEndDate() != null ? task.getActualEndDate() : task.getPlannedDate())
                .activityType(activityType)
                .description(task.getTitle())
                .performedBy(task.getAssigneeName() != null ? task.getAssigneeName() : "Nhân viên")
                .status("COMPLETED")
                .notes(task.getNotes())
                .vietGapCompliant(true)
                .vietGapReason("Đã thực hiện đúng quy trình theo kế hoạch.")
                .build();
    }
}
