package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.Getter;
import org.example.QuanLyMuaVu.Enums.TaskStatus;

/**
 * Observer Pattern: org.example.QuanLyMuaVu.module.season.entity.Task Completed Event.
 * <p>
 * Published when a task transitions to DONE status.
 * Listeners can use this to:
 * - Update season progress tracking
 * - Trigger follow-up tasks
 * - Send completion notifications
 * - Log field activity
 */
@Getter
public class TaskCompletedEvent extends DomainEvent {

    private final Integer taskId;
    private final String taskTitle;
    private final Integer seasonId;
    private final Long assigneeUserId;
    private final Long ownerUserId;
    private final TaskStatus previousStatus;

    public TaskCompletedEvent(org.example.QuanLyMuaVu.module.season.entity.Task task, TaskStatus previousStatus) {
        super("org.example.QuanLyMuaVu.module.season.entity.Task", task.getId() != null ? task.getId().toString() : "unknown");
        this.taskId = task.getId();
        this.taskTitle = task.getTitle();
        this.seasonId = task.getSeason() != null ? task.getSeason().getId() : null;
        this.assigneeUserId = task.getUser() != null ? task.getUser().getId() : null;
        this.ownerUserId = task.getSeason() != null
                && task.getSeason().getPlot() != null
                && task.getSeason().getPlot().getFarm() != null
                && task.getSeason().getPlot().getFarm().getUser() != null
                        ? task.getSeason().getPlot().getFarm().getUser().getId()
                        : null;
        this.previousStatus = previousStatus;
    }

    @Override
    public String getEventType() {
        return "TASK_COMPLETED";
    }
}
