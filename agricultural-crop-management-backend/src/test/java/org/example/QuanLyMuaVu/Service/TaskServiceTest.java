package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.season.dto.request.TaskRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.entity.Task;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for TaskService.
 * 
 * Covers key operations: create, update, delete, get tasks.
 */
@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private IdentityQueryPort identityQueryPort;

    @Mock
    private SeasonRepository seasonRepository;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Season testSeason;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("farmer")
                .email("farmer@test.com")
                .build();

        testSeason = Season.builder()
                .id(1)
                .seasonName("Spring 2024")
                .build();

        testTask = Task.builder()
                .id(1)
                .user(testUser)
                .season(testSeason)
                .title("Apply Fertilizer")
                .description("Apply NPK fertilizer to field A")
                .plannedDate(LocalDate.now().plusDays(1))
                .dueDate(LocalDate.now().plusDays(3))
                .status(TaskStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Create - Creates task with valid data and defaults to PENDING status")
    void create_WithValidData_ReturnsTaskResponse() {
        // Arrange
        TaskRequest request = TaskRequest.builder()
                .userId(1L)
                .seasonId(1)
                .title("Water Plants")
                .description("Morning irrigation")
                .plannedDate(LocalDate.now().plusDays(1))
                .dueDate(LocalDate.now().plusDays(2))
                .status(null) // Should default to PENDING
                .build();

        when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
        when(seasonRepository.findById(1)).thenReturn(Optional.of(testSeason));

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        when(taskRepository.save(taskCaptor.capture())).thenAnswer(i -> {
            Task t = i.getArgument(0);
            t.setId(1);
            return t;
        });

        // Act
        TaskResponse response = taskService.create(request);

        // Assert
        assertNotNull(response);

        Task captured = taskCaptor.getValue();
        assertEquals("Water Plants", captured.getTitle());
        assertEquals(TaskStatus.PENDING, captured.getStatus()); // Default status
        assertEquals(testUser, captured.getUser());
        assertEquals(testSeason, captured.getSeason());
    }

    @Test
    @DisplayName("Create - Creates task without season (season is optional)")
    void create_WithoutSeason_CreatesSuccessfully() {
        // Arrange
        TaskRequest request = TaskRequest.builder()
                .userId(1L)
                .seasonId(null) // No season
                .title("General Task")
                .status(TaskStatus.PENDING)
                .build();

        when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any())).thenAnswer(i -> {
            Task t = i.getArgument(0);
            t.setId(1);
            return t;
        });

        // Act
        TaskResponse response = taskService.create(request);

        // Assert
        assertNotNull(response);
        assertNull(response.getSeasonId());
        verify(seasonRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Create - Throws NoSuchElementException when user not found")
    void create_WhenUserNotFound_ThrowsException() {
        // Arrange
        TaskRequest request = TaskRequest.builder()
                .userId(999L)
                .title("Task")
                .build();

        when(identityQueryPort.findUserById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class,
                () -> taskService.create(request));

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update - Updates task status correctly")
    void update_WithNewStatus_UpdatesStatusCorrectly() {
        // Arrange
        TaskRequest updateRequest = TaskRequest.builder()
                .title("Updated Task")
                .description("Updated description")
                .plannedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(5))
                .status(TaskStatus.IN_PROGRESS)
                .build();

        when(taskRepository.findById(1)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        TaskResponse response = taskService.update(1, updateRequest);

        // Assert
        assertNotNull(response);
        verify(taskRepository).save(argThat(task -> task.getStatus() == TaskStatus.IN_PROGRESS &&
                "Updated Task".equals(task.getTitle())));
    }

    @Test
    @DisplayName("GetAll - Returns all tasks")
    void getAll_ReturnsAllTasks() {
        // Arrange
        Task task2 = Task.builder()
                .id(2)
                .user(testUser)
                .title("Second Task")
                .status(TaskStatus.DONE)
                .build();

        when(taskRepository.findAll()).thenReturn(List.of(testTask, task2));

        // Act
        List<TaskResponse> result = taskService.getAll();

        // Assert
        assertEquals(2, result.size());
    }
}
