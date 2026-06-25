package org.example.season.strategy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.example.season.enums.TaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskStatusStrategy implements StatusTransitionStrategy<TaskStatus> {

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        TRANSITIONS.put(TaskStatus.PENDING,
                EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE, TaskStatus.CANCELLED));
        TRANSITIONS.put(TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.DONE, TaskStatus.OVERDUE, TaskStatus.CANCELLED));
        TRANSITIONS.put(TaskStatus.OVERDUE, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.CANCELLED));
        TRANSITIONS.put(TaskStatus.DONE, EnumSet.noneOf(TaskStatus.class));
        TRANSITIONS.put(TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class));
    }

    @Override
    public boolean canTransition(TaskStatus currentStatus, TaskStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return true;
        }
        Set<TaskStatus> allowed = TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    @Override
    public Set<TaskStatus> getAllowedTransitions(TaskStatus currentStatus) {
        if (currentStatus == null) {
            return EnumSet.noneOf(TaskStatus.class);
        }
        Set<TaskStatus> allowed = TRANSITIONS.get(currentStatus);
        return allowed != null ? EnumSet.copyOf(allowed) : EnumSet.noneOf(TaskStatus.class);
    }

    @Override
    public boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.DONE || status == TaskStatus.CANCELLED;
    }

    @Override
    public TaskStatus getInitialStatus() {
        return TaskStatus.PENDING;
    }
}
