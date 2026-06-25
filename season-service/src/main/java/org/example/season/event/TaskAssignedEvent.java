package org.example.season.event;

import lombok.Getter;
import org.example.season.entity.Task;

@Getter
public class TaskAssignedEvent extends DomainEvent {

    private final Integer taskId;
    private final String taskTitle;
    private final Integer seasonId;
    private final Long assigneeUserId;

    public TaskAssignedEvent(Task task) {
        super("Task", task.getId() != null ? task.getId().toString() : "unknown");
        this.taskId = task.getId();
        this.taskTitle = task.getTitle();
        this.seasonId = task.getSeason() != null ? task.getSeason().getId() : null;
        this.assigneeUserId = task.getUserId();
    }

    @Override
    public String getEventType() {
        return "TASK_ASSIGNED";
    }
}
