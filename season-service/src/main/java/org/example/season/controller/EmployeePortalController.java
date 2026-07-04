package org.example.season.controller;

import lombok.RequiredArgsConstructor;
import org.example.season.service.TaskAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
public class EmployeePortalController {

    private final TaskAssignmentService taskAssignmentService;

    @GetMapping("/tasks/my-tasks")
    public ResponseEntity<?> getMyTasks() {
        Long employeeUserId = Long.parseLong(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        return ResponseEntity.ok(taskAssignmentService.getTasksForEmployee(employeeUserId));
    }
}
