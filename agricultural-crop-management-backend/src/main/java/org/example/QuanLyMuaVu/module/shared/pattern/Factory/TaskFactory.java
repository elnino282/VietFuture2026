package org.example.QuanLyMuaVu.module.shared.pattern.Factory;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.shared.pattern.Strategy.StatusTransitionStrategy;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateTaskRequest;
import org.springframework.stereotype.Component;

/**
 * Factory Method Pattern: org.example.QuanLyMuaVu.module.season.entity.Task Factory.
 * <p>
 * Creates org.example.QuanLyMuaVu.module.season.entity.Task entities with proper defaults.
 * Uses Strategy pattern for initial status.
 */
@Component
@RequiredArgsConstructor
public class TaskFactory implements EntityFactory<org.example.QuanLyMuaVu.module.season.entity.Task, CreateTaskRequest> {

    private final StatusTransitionStrategy<TaskStatus> statusStrategy;

    @Override
    public org.example.QuanLyMuaVu.module.season.entity.Task create(CreateTaskRequest request, org.example.QuanLyMuaVu.module.identity.entity.User creator) {
        org.example.QuanLyMuaVu.module.season.entity.Task task = new org.example.QuanLyMuaVu.module.season.entity.Task();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(statusStrategy.getInitialStatus());

        // Set dates
        task.setPlannedDate(request.getPlannedDate());
        task.setDueDate(request.getDueDate());

        // Set the user who created/owns this task
        task.setUser(creator);

        return task;
    }

    /**
     * Creates a org.example.QuanLyMuaVu.module.season.entity.Task linked to a org.example.QuanLyMuaVu.module.season.entity.Season.
     */
    public org.example.QuanLyMuaVu.module.season.entity.Task createWithSeason(CreateTaskRequest request, org.example.QuanLyMuaVu.module.season.entity.Season season, org.example.QuanLyMuaVu.module.identity.entity.User creator) {
        org.example.QuanLyMuaVu.module.season.entity.Task task = create(request, creator);
        task.setSeason(season);
        return task;
    }
}
