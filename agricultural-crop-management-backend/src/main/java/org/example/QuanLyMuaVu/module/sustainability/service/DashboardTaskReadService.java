package org.example.QuanLyMuaVu.module.sustainability.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.TodayTaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardTaskReadService {

    private static final List<org.example.QuanLyMuaVu.Enums.TaskStatus> COMPLETED_STATUSES = List.of(
            org.example.QuanLyMuaVu.Enums.TaskStatus.DONE,
            org.example.QuanLyMuaVu.Enums.TaskStatus.CANCELLED);

    private final CurrentUserService currentUserService;
    private final IdentityQueryPort identityQueryPort;
    private final TaskQueryPort taskQueryPort;

    public Page<TodayTaskResponse> getTodayTasks(Integer seasonId, Pageable pageable) {
        org.example.QuanLyMuaVu.module.identity.entity.User user = resolveCurrentUser();
        LocalDate today = LocalDate.now();
        Page<org.example.QuanLyMuaVu.module.season.entity.DashboardTaskView> tasks = taskQueryPort.findTodayTasksByUser(
                user.getId(), seasonId, today, pageable);
        return tasks.map(this::mapToTodayTaskResponse);
    }

    public List<TodayTaskResponse> getUpcomingTasks(int days, Integer seasonId) {
        org.example.QuanLyMuaVu.module.identity.entity.User user = resolveCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate untilDate = today.plusDays(days);
        List<org.example.QuanLyMuaVu.module.season.entity.DashboardTaskView> tasks = taskQueryPort.findUpcomingTasksByUser(
                user.getId(), seasonId, today, untilDate, COMPLETED_STATUSES);
        return tasks.stream().map(this::mapToTodayTaskResponse).toList();
    }

    private org.example.QuanLyMuaVu.module.identity.entity.User resolveCurrentUser() {
        Long ownerId = currentUserService.getCurrentUserId();
        return identityQueryPort.findUserById(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private TodayTaskResponse mapToTodayTaskResponse(org.example.QuanLyMuaVu.module.season.entity.DashboardTaskView task) {
        String plotName = task.getPlotName() != null ? task.getPlotName() : "";
        String type = inferTaskType(task.getTitle(), task.getDescription());
        LocalDate dueDate = task.getDueDate() != null ? task.getDueDate() : task.getPlannedDate();
        return TodayTaskResponse.builder()
                .taskId(task.getTaskId())
                .title(task.getTitle())
                .plotName(plotName)
                .type(type)
                .assigneeName(task.getAssigneeName() != null ? task.getAssigneeName() : "")
                .dueDate(dueDate)
                .status(task.getStatus() != null ? task.getStatus().name() : "")
                .build();
    }

    private String inferTaskType(String title, String description) {
        String text = String.format("%s %s",
                title != null ? title : "",
                description != null ? description : "").toLowerCase(Locale.ROOT);
        if (text.contains("irrigat") || text.contains("water")) {
            return "irrigation";
        }
        if (text.contains("fertil") || text.contains("npk")) {
            return "fertilizing";
        }
        if (text.contains("spray") || text.contains("pest") || text.contains("insect")) {
            return "spraying";
        }
        if (text.contains("harvest") || text.contains("collect")) {
            return "harvesting";
        }
        if (text.contains("inspect") || text.contains("scout")) {
            return "scouting";
        }
        return "scouting";
    }
}
