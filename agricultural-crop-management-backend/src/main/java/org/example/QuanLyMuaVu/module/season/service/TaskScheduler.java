package org.example.QuanLyMuaVu.module.season.service;

import java.time.LocalDate;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job to auto-update overdue tasks daily.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TaskScheduler {

    TaskRepository taskRepository;

    /**
     * Run daily at 00:05 to mark tasks as OVERDUE.
     */
    @Scheduled(cron = " 0 5 0 * * ?")
    @Transactional
    public void updateOverdueTasks() {
        LocalDate today = LocalDate.now();

        int updated = taskRepository.updateOverdueTasks(
                today,
                TaskStatus.OVERDUE,
                Arrays.asList(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));

        log.info("Overdue task scheduler: Updated {} tasks to OVERDUE status", updated);
    }
}
