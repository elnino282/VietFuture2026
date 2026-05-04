package org.example.QuanLyMuaVu.module.shared.pattern.Strategy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.springframework.stereotype.Component;

/**
 * Strategy Pattern Implementation: Task Status Transitions.
 * <p>
 * Encapsulates the state machine for Task lifecycle:
 * 
 * <pre>
 *     PENDING ──→ IN_PROGRESS ──→ DONE
 *        │            │            │
 *        │            ↓            │
 *        │         OVERDUE         │
 *        │            │            │
 *        └────────────┴────────────┴──→ CANCELLED
 * </pre>
 * <p>
 * Business Rules:
 * - PENDING: Initial state for newly created tasks
 * - IN_PROGRESS: Task work has started (actual_start_date set)
 * - DONE: Task completed (actual_end_date set)
 * - OVERDUE: Task passed due date but not completed
 * - CANCELLED: Task terminated (can happen from any non-terminal state)
 */
@Component
public class TaskStatusStrategy implements StatusTransitionStrategy<TaskStatus> {

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        // PENDING can go to IN_PROGRESS, OVERDUE, or CANCELLED
        TRANSITIONS.put(TaskStatus.PENDING,
                EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE, TaskStatus.CANCELLED));

        // IN_PROGRESS can go to DONE, OVERDUE, or CANCELLED
        TRANSITIONS.put(TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.DONE, TaskStatus.OVERDUE, TaskStatus.CANCELLED));

        // OVERDUE can go to IN_PROGRESS, DONE, or CANCELLED (allow recovery)
        TRANSITIONS.put(TaskStatus.OVERDUE, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE, TaskStatus.CANCELLED));

        // DONE is terminal
        TRANSITIONS.put(TaskStatus.DONE, EnumSet.noneOf(TaskStatus.class));

        // CANCELLED is terminal
        TRANSITIONS.put(TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class));
    }

    @Override
    public boolean canTransition(TaskStatus currentStatus, TaskStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return true; // No-op transitions are allowed
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
