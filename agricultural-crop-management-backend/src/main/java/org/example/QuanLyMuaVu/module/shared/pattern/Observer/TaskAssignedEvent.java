package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.Getter;

@Getter
public class TaskAssignedEvent extends DomainEvent {

    private final Integer taskId;
    private final String taskTitle;
    private final Integer seasonId;
    private final Long assigneeUserId;
    private final Long ownerUserId;
    private final Long assignedByUserId;

    public TaskAssignedEvent(org.example.QuanLyMuaVu.module.season.entity.Task task, Long assignedByUserId) {
        super("org.example.QuanLyMuaVu.module.season.entity.Task", task != null && task.getId() != null ? task.getId().toString() : "unknown");
        this.taskId = task != null ? task.getId() : null;
        this.taskTitle = task != null ? task.getTitle() : null;
        this.seasonId = task != null && task.getSeason() != null ? task.getSeason().getId() : null;
        this.assigneeUserId = task != null && task.getUser() != null ? task.getUser().getId() : null;
        this.ownerUserId = task != null
                && task.getSeason() != null
                && task.getSeason().getPlot() != null
                && task.getSeason().getPlot().getFarm() != null
                && task.getSeason().getPlot().getFarm().getUser() != null
                        ? task.getSeason().getPlot().getFarm().getUser().getId()
                        : null;
        this.assignedByUserId = assignedByUserId;
    }

    @Override
    public String getEventType() {
        return "TASK_ASSIGNED";
    }
}
