package org.example.QuanLyMuaVu.module.season.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.TaskAssignedEvent;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.TaskCompletedEvent;
import org.example.QuanLyMuaVu.Constant.PredefinedRole;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.incident.port.IncidentCommandPort;
import org.example.QuanLyMuaVu.module.season.dto.request.AddSeasonEmployeeRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.BulkAssignSeasonEmployeesRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.EmployeeTaskProgressRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdatePayrollRecordRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateSeasonEmployeeRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.EmployeeDirectoryResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.PayrollRecordResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.SeasonEmployeeResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskProgressLogResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.TaskResponse;
import org.example.QuanLyMuaVu.module.season.entity.PayrollRecord;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.entity.SeasonEmployee;
import org.example.QuanLyMuaVu.module.season.entity.Task;
import org.example.QuanLyMuaVu.module.season.entity.TaskProgressLog;
import org.example.QuanLyMuaVu.module.season.repository.PayrollRecordRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonEmployeeRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskProgressLogRepository;
import org.example.QuanLyMuaVu.module.season.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class LaborManagementService {

    static final BigDecimal DEFAULT_WAGE_PER_TASK = BigDecimal.valueOf(150_000L);

    SeasonRepository seasonRepository;
    TaskRepository taskRepository;
    IdentityQueryPort identityQueryPort;
    SeasonEmployeeRepository seasonEmployeeRepository;
    TaskProgressLogRepository taskProgressLogRepository;
    PayrollRecordRepository payrollRecordRepository;
    IncidentCommandPort incidentCommandPort;
    FarmAccessPort farmAccessService;
    AuditLogService auditLogService;
    DomainEventPublisher domainEventPublisher;

    // ---------------------------------------------------------------------
    // Farmer-side management
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDirectoryResponse> listEmployeeDirectory(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending().and(Sort.by("id").ascending()));
        Page<org.example.QuanLyMuaVu.module.identity.entity.User> employees = identityQueryPort.searchUsersByRoleAndStatusAndKeyword(
                PredefinedRole.EMPLOYEE_ROLE,
                UserStatus.ACTIVE,
                keyword,
                pageable);

        List<EmployeeDirectoryResponse> items = employees.getContent().stream()
                .map(this::toEmployeeDirectoryResponse)
                .toList();

        return PageResponse.of(employees, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<SeasonEmployeeResponse> listSeasonEmployees(Integer seasonId, String keyword, int page, int size) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending()));
        Page<SeasonEmployee> employees = seasonEmployeeRepository.searchBySeasonAndKeyword(season.getId(), keyword, pageable);
        List<SeasonEmployeeResponse> items = employees.getContent().stream()
                .map(this::toSeasonEmployeeResponse)
                .toList();
        return PageResponse.of(employees, items);
    }

    public SeasonEmployeeResponse addSeasonEmployee(Integer seasonId, AddSeasonEmployeeRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonOpenForLabor(season);
        org.example.QuanLyMuaVu.module.identity.entity.User employee = resolveActiveEmployee(request.getEmployeeUserId());

        if (seasonEmployeeRepository.existsBySeason_IdAndEmployee_Id(season.getId(), employee.getId())) {
            throw new AppException(ErrorCode.SEASON_EMPLOYEE_ALREADY_EXISTS);
        }

        org.example.QuanLyMuaVu.module.identity.entity.User currentFarmer = farmAccessService.getCurrentUser();

        SeasonEmployee seasonEmployee = SeasonEmployee.builder()
                .season(season)
                .employee(employee)
                .addedBy(currentFarmer)
                .wagePerTask(request.getWagePerTask())
                .active(true)
                .build();

        SeasonEmployee saved = seasonEmployeeRepository.save(seasonEmployee);

        notifyUser(employee,
                "Added to season workforce",
                String.format("You have been added to season %s.", season.getSeasonName()),
                "/employee/tasks");

        return toSeasonEmployeeResponse(saved);
    }

    public List<SeasonEmployeeResponse> bulkAssignSeasonEmployees(Integer seasonId, BulkAssignSeasonEmployeesRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonOpenForLabor(season);

        org.example.QuanLyMuaVu.module.identity.entity.User currentFarmer = farmAccessService.getCurrentUser();
        Set<Long> uniqueEmployeeIds = new HashSet<>(request.getEmployeeUserIds());
        List<SeasonEmployeeResponse> responses = new ArrayList<>();

        for (Long employeeUserId : uniqueEmployeeIds) {
            org.example.QuanLyMuaVu.module.identity.entity.User employee = resolveActiveEmployee(employeeUserId);
            if (seasonEmployeeRepository.existsBySeason_IdAndEmployee_Id(season.getId(), employee.getId())) {
                continue;
            }

            SeasonEmployee seasonEmployee = SeasonEmployee.builder()
                    .season(season)
                    .employee(employee)
                    .addedBy(currentFarmer)
                    .wagePerTask(request.getWagePerTask())
                    .active(true)
                    .build();

            SeasonEmployee saved = seasonEmployeeRepository.save(seasonEmployee);
            responses.add(toSeasonEmployeeResponse(saved));

            notifyUser(employee,
                    "Added to season workforce",
                    String.format("You have been added to season %s.", season.getSeasonName()),
                    "/employee/tasks");
        }

        return responses;
    }

    public SeasonEmployeeResponse updateSeasonEmployee(Integer seasonId, Long employeeUserId, UpdateSeasonEmployeeRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonOpenForLabor(season);
        SeasonEmployee seasonEmployee = seasonEmployeeRepository.findBySeason_IdAndEmployee_Id(season.getId(), employeeUserId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND));
        BigDecimal beforeWagePerTask = seasonEmployee.getWagePerTask();

        if (request.getWagePerTask() != null) {
            seasonEmployee.setWagePerTask(request.getWagePerTask());
        }
        if (request.getActive() != null) {
            seasonEmployee.setActive(request.getActive());
        }

        SeasonEmployee saved = seasonEmployeeRepository.save(seasonEmployee);

        if (hasBigDecimalChanged(beforeWagePerTask, saved.getWagePerTask())) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("seasonEmployeeId", saved.getId());
            snapshot.put("seasonId", saved.getSeason() != null ? saved.getSeason().getId() : null);
            snapshot.put("employeeUserId", saved.getEmployee() != null ? saved.getEmployee().getId() : null);
            snapshot.put("beforeWagePerTask", beforeWagePerTask);
            snapshot.put("afterWagePerTask", saved.getWagePerTask());

            auditLogService.logModuleOperation(
                    "WORKFORCE",
                    "SEASON_EMPLOYEE",
                    saved.getId(),
                    "PAYROLL_WAGE_PER_TASK_UPDATED",
                    resolveAuditActor(),
                    snapshot,
                    "Farmer updated wage per task",
                    null);
        }

        return toSeasonEmployeeResponse(saved);
    }

    public void removeSeasonEmployee(Integer seasonId, Long employeeUserId) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonOpenForLabor(season);
        SeasonEmployee seasonEmployee = seasonEmployeeRepository.findBySeason_IdAndEmployee_Id(season.getId(), employeeUserId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND));
        seasonEmployeeRepository.delete(seasonEmployee);
    }

    public TaskResponse assignTaskToEmployee(Integer taskId, Long employeeUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
        Season season = task.getSeason();
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        ensureSeasonOpenForLabor(season);

        farmAccessService.assertCurrentUserCanAccessSeason(season);

        SeasonEmployee seasonEmployee = seasonEmployeeRepository.findBySeason_IdAndEmployee_Id(season.getId(), employeeUserId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND));

        if (!Boolean.TRUE.equals(seasonEmployee.getActive())) {
            throw new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND);
        }

        org.example.QuanLyMuaVu.module.identity.entity.User currentFarmer = farmAccessService.getCurrentUser();

        task.setUser(seasonEmployee.getEmployee());
        Task saved = taskRepository.save(task);

        domainEventPublisher.publish(new TaskAssignedEvent(saved, currentFarmer.getId()));

        return toTaskResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskProgressLogResponse> listSeasonProgress(Integer seasonId, Long employeeUserId, Integer taskId,
            int page, int size) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("loggedAt").descending().and(Sort.by("id").descending()));
        Page<TaskProgressLog> logs = taskProgressLogRepository.findBySeasonFilters(season.getId(), employeeUserId, taskId, pageable);
        List<TaskProgressLogResponse> items = logs.getContent().stream()
                .map(this::toTaskProgressLogResponse)
                .toList();
        return PageResponse.of(logs, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<PayrollRecordResponse> listSeasonPayroll(Integer seasonId, Long employeeUserId, int page, int size) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("periodStart").descending().and(Sort.by("id").descending()));
        Page<PayrollRecord> payroll = payrollRecordRepository.findBySeasonAndEmployee(season.getId(), employeeUserId, pageable);
        List<PayrollRecordResponse> items = payroll.getContent().stream()
                .map(this::toPayrollRecordResponse)
                .toList();
        return PageResponse.of(payroll, items);
    }

    @Transactional(readOnly = true)
    public PayrollRecordResponse getSeasonPayrollDetail(Integer seasonId, Integer payrollRecordId) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        PayrollRecord payrollRecord = payrollRecordRepository.findByIdAndSeason_Id(payrollRecordId, season.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return toPayrollRecordResponse(payrollRecord);
    }

    public PayrollRecordResponse updateSeasonPayroll(
            Integer seasonId,
            Integer payrollRecordId,
            UpdatePayrollRecordRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        PayrollRecord payrollRecord = payrollRecordRepository.findByIdAndSeason_Id(payrollRecordId, season.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        String beforeNote = normalizePayrollNote(payrollRecord.getNote());
        String afterNote = normalizePayrollNote(request.getNote());
        payrollRecord.setNote(afterNote);
        PayrollRecord saved = payrollRecordRepository.save(payrollRecord);

        if (!Objects.equals(beforeNote, afterNote)) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("payrollRecordId", saved.getId());
            snapshot.put("seasonId", season.getId());
            snapshot.put("employeeUserId", saved.getEmployee() != null ? saved.getEmployee().getId() : null);
            snapshot.put("periodStart", saved.getPeriodStart());
            snapshot.put("periodEnd", saved.getPeriodEnd());
            snapshot.put("beforeNote", beforeNote);
            snapshot.put("afterNote", afterNote);

            auditLogService.logModuleOperation(
                    "WORKFORCE",
                    "PAYROLL_RECORD",
                    saved.getId(),
                    "PAYROLL_NOTE_UPDATED",
                    resolveAuditActor(),
                    snapshot,
                    "Farmer updated payroll note",
                    null);
        }

        return toPayrollRecordResponse(saved);
    }

    public List<PayrollRecordResponse> recalculatePayroll(Integer seasonId, Long employeeUserId, LocalDate periodStart,
            LocalDate periodEnd) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        ensureSeasonOpenForLabor(season);
        LocalDate resolvedStart = periodStart != null ? periodStart : LocalDate.now().withDayOfMonth(1);
        LocalDate resolvedEnd = periodEnd != null ? periodEnd : resolvedStart.withDayOfMonth(resolvedStart.lengthOfMonth());

        if (resolvedEnd.isBefore(resolvedStart)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        List<SeasonEmployee> targets = new ArrayList<>();
        if (employeeUserId != null) {
            SeasonEmployee employee = seasonEmployeeRepository.findBySeason_IdAndEmployee_Id(season.getId(), employeeUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND));
            targets.add(employee);
        } else {
            targets.addAll(seasonEmployeeRepository.findAllBySeason_IdAndActiveTrue(season.getId()));
        }

        List<PayrollRecordResponse> responses = new ArrayList<>();
        for (SeasonEmployee target : targets) {
            PayrollRecord record = recalculatePayrollRecord(target, season, resolvedStart, resolvedEnd);
            responses.add(toPayrollRecordResponse(record));
        }
        return responses;
    }

    // ---------------------------------------------------------------------
    // Employee-side portal
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listAssignedTasksForCurrentEmployee(String status, Integer seasonId, int page, int size) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentEmployee = farmAccessService.getCurrentUser();
        TaskStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            statusFilter = TaskStatus.fromCode(status);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending().and(Sort.by("id").descending()));
        Page<Task> taskPage = taskRepository.findByUserWithFilters(currentEmployee, statusFilter, seasonId, null, pageable);

        List<TaskResponse> items = taskPage.getContent().stream()
                .map(this::toTaskResponse)
                .toList();
        return PageResponse.of(taskPage, items);
    }

    public TaskResponse acceptTask(Integer taskId) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentEmployee = farmAccessService.getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
        ensureTaskBelongsToEmployee(task, currentEmployee);
        ensureSeasonOpenForEmployeeTaskWrite(task.getSeason());

        if (task.getStatus() == TaskStatus.CANCELLED || task.getStatus() == TaskStatus.DONE) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.OVERDUE) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            if (task.getActualStartDate() == null) {
                task.setActualStartDate(LocalDate.now());
            }
        }

        Task saved = taskRepository.save(task);
        return toTaskResponse(saved);
    }

    public TaskProgressLogResponse reportTaskProgress(Integer taskId, EmployeeTaskProgressRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentEmployee = farmAccessService.getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
        ensureTaskBelongsToEmployee(task, currentEmployee);
        ensureSeasonOpenForEmployeeTaskWrite(task.getSeason());
        TaskStatus previousStatus = task.getStatus();

        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            if (task.getActualStartDate() == null) {
                task.setActualStartDate(LocalDate.now());
            }
        }

        if (request.getProgressPercent() >= 100) {
            task.setStatus(TaskStatus.DONE);
            task.setActualEndDate(LocalDate.now());
            if (task.getActualStartDate() == null) {
                task.setActualStartDate(LocalDate.now());
            }
        } else if (task.getStatus() != TaskStatus.DONE) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        taskRepository.save(task);
        if (previousStatus != TaskStatus.DONE && task.getStatus() == TaskStatus.DONE) {
            domainEventPublisher.publish(new TaskCompletedEvent(task, previousStatus));
        }

        TaskProgressLog logEntry = TaskProgressLog.builder()
                .task(task)
                .employee(currentEmployee)
                .progressPercent(request.getProgressPercent())
                .note(request.getNote())
                .evidenceUrl(request.getEvidenceUrl())
                .loggedAt(LocalDateTime.now())
                .build();

        TaskProgressLog saved = taskProgressLogRepository.save(logEntry);

        if (task.getSeason() != null && task.getSeason().getPlot() != null && task.getSeason().getPlot().getFarm() != null) {
            org.example.QuanLyMuaVu.module.identity.entity.User farmOwner = task.getSeason().getPlot().getFarm().getUser();
            if (farmOwner != null && !farmOwner.getId().equals(currentEmployee.getId())) {
                notifyUser(farmOwner,
                        "Task progress updated",
                        String.format("%s updated task '%s' to %d%%.",
                                currentEmployee.getFullName() != null ? currentEmployee.getFullName()
                                        : currentEmployee.getUsername(),
                                task.getTitle(),
                                request.getProgressPercent()),
                        "/farmer/labor-management");
            }
        }

        recalculatePayrollForTask(task);
        return toTaskProgressLogResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskProgressLogResponse> listMyProgress(int page, int size) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentEmployee = farmAccessService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("loggedAt").descending().and(Sort.by("id").descending()));
        Page<TaskProgressLog> logs = taskProgressLogRepository.findByEmployeeId(currentEmployee.getId(), pageable);
        List<TaskProgressLogResponse> items = logs.getContent().stream()
                .map(this::toTaskProgressLogResponse)
                .toList();
        return PageResponse.of(logs, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<PayrollRecordResponse> listMyPayroll(Integer seasonId, int page, int size) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentEmployee = farmAccessService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("periodStart").descending().and(Sort.by("id").descending()));
        Page<PayrollRecord> payroll = payrollRecordRepository.findByEmployeeAndSeason(currentEmployee.getId(), seasonId, pageable);
        List<PayrollRecordResponse> items = payroll.getContent().stream()
                .map(this::toPayrollRecordResponse)
                .toList();
        return PageResponse.of(payroll, items);
    }

    @Transactional(readOnly = true)
    public PayrollRecordResponse getMyPayrollDetail(Integer payrollRecordId) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentEmployee = farmAccessService.getCurrentUser();
        PayrollRecord payrollRecord = payrollRecordRepository.findByIdAndEmployee_Id(payrollRecordId, currentEmployee.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return toPayrollRecordResponse(payrollRecord);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getSeasonPlanForCurrentEmployee(Integer seasonId) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentEmployee = farmAccessService.getCurrentUser();
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));

        SeasonEmployee assignment = seasonEmployeeRepository.findBySeason_IdAndEmployee_Id(season.getId(), currentEmployee.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND));

        if (!Boolean.TRUE.equals(assignment.getActive())) {
            throw new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND);
        }

        return taskRepository.findAllBySeason_Id(season.getId()).stream()
                .sorted(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Task::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toTaskResponse)
                .toList();
    }

    public void syncPayrollForTask(Task task) {
        recalculatePayrollForTask(task);
    }

    // ---------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------

    private boolean isSeasonLocked(Season season) {
        if (season == null || season.getStatus() == null) {
            return false;
        }
        return season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED;
    }

    private void ensureSeasonOpenForLabor(Season season) {
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        if (isSeasonLocked(season)) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }
    }

    private void ensureSeasonOpenForEmployeeTaskWrite(Season season) {
        if (season == null) {
            return;
        }
        if (isSeasonLocked(season)) {
            throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_MODIFY_TASK);
        }
    }

    private void ensureTaskBelongsToEmployee(Task task, org.example.QuanLyMuaVu.module.identity.entity.User employee) {
        if (task.getUser() == null || task.getUser().getId() == null || !task.getUser().getId().equals(employee.getId())) {
            throw new AppException(ErrorCode.TASK_NOT_ASSIGNED_TO_EMPLOYEE);
        }
    }

    private Season getSeasonForCurrentFarmer(Integer seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private org.example.QuanLyMuaVu.module.identity.entity.User resolveActiveEmployee(Long employeeUserId) {
        org.example.QuanLyMuaVu.module.identity.entity.User employee = identityQueryPort.findUserById(employeeUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (employee.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        boolean hasEmployeeRole = identityQueryPort
                .existsUserByIdAndRoleCode(employee.getId(), PredefinedRole.EMPLOYEE_ROLE);
        if (!hasEmployeeRole) {
            throw new AppException(ErrorCode.EMPLOYEE_ROLE_REQUIRED);
        }
        return employee;
    }

    private PayrollRecord recalculatePayrollRecord(SeasonEmployee seasonEmployee, Season season, LocalDate periodStart,
            LocalDate periodEnd) {
        List<Task> tasks = taskRepository.findAllBySeason_IdAndUser_Id(season.getId(), seasonEmployee.getEmployee().getId());

        List<Task> periodTasks = tasks.stream()
                .filter(task -> {
                    LocalDate date = resolvePayrollDate(task);
                    return date != null && !date.isBefore(periodStart) && !date.isAfter(periodEnd);
                })
                .toList();

        int totalAssigned = periodTasks.size();
        int totalCompleted = (int) periodTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();

        BigDecimal wagePerTask = seasonEmployee.getWagePerTask() != null
                ? seasonEmployee.getWagePerTask()
                : DEFAULT_WAGE_PER_TASK;
        BigDecimal totalAmount = wagePerTask.multiply(BigDecimal.valueOf(totalCompleted));

        PayrollRecord payrollRecord = payrollRecordRepository
                .findByEmployee_IdAndSeason_IdAndPeriodStartAndPeriodEnd(
                        seasonEmployee.getEmployee().getId(),
                        season.getId(),
                        periodStart,
                        periodEnd)
                .orElse(PayrollRecord.builder()
                        .employee(seasonEmployee.getEmployee())
                        .season(season)
                        .periodStart(periodStart)
                        .periodEnd(periodEnd)
                        .build());

        payrollRecord.setTotalAssignedTasks(totalAssigned);
        payrollRecord.setTotalCompletedTasks(totalCompleted);
        payrollRecord.setWagePerTask(wagePerTask);
        payrollRecord.setTotalAmount(totalAmount);
        payrollRecord.setGeneratedAt(LocalDateTime.now());
        payrollRecord.setNote("Auto-calculated from task completion data.");

        return payrollRecordRepository.save(payrollRecord);
    }

    private LocalDate resolvePayrollDate(Task task) {
        if (task.getActualEndDate() != null) {
            return task.getActualEndDate();
        }
        if (task.getDueDate() != null) {
            return task.getDueDate();
        }
        if (task.getPlannedDate() != null) {
            return task.getPlannedDate();
        }
        if (task.getCreatedAt() != null) {
            return task.getCreatedAt().toLocalDate();
        }
        return null;
    }

    private void recalculatePayrollForTask(Task task) {
        if (task.getSeason() == null || task.getUser() == null || task.getUser().getId() == null) {
            return;
        }
        if (!identityQueryPort.existsUserByIdAndRoleCode(task.getUser().getId(), PredefinedRole.EMPLOYEE_ROLE)) {
            return;
        }
        seasonEmployeeRepository.findBySeason_IdAndEmployee_Id(task.getSeason().getId(), task.getUser().getId())
                .ifPresent(seasonEmployee -> {
                    LocalDate dateAnchor = task.getActualEndDate() != null ? task.getActualEndDate() : LocalDate.now();
                    LocalDate periodStart = dateAnchor.withDayOfMonth(1);
                    LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
                    recalculatePayrollRecord(seasonEmployee, task.getSeason(), periodStart, periodEnd);
                });
    }

    private void notifyUser(org.example.QuanLyMuaVu.module.identity.entity.User user, String title, String message, String link) {
        if (user == null || user.getId() == null) {
            return;
        }
        incidentCommandPort.createNotification(user.getId(), title, message, link);
    }

    private String resolveAuditActor() {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = farmAccessService.getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        if (currentUser.getUsername() != null && !currentUser.getUsername().isBlank()) {
            return currentUser.getUsername();
        }
        if (currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
            return currentUser.getEmail();
        }
        return currentUser.getId() != null ? currentUser.getId().toString() : null;
    }

    private boolean hasBigDecimalChanged(BigDecimal before, BigDecimal after) {
        if (before == null && after == null) {
            return false;
        }
        if (before == null || after == null) {
            return true;
        }
        return before.compareTo(after) != 0;
    }

    private String normalizePayrollNote(String note) {
        if (note == null) {
            return null;
        }
        String normalized = note.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private EmployeeDirectoryResponse toEmployeeDirectoryResponse(org.example.QuanLyMuaVu.module.identity.entity.User user) {
        return EmployeeDirectoryResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    private SeasonEmployeeResponse toSeasonEmployeeResponse(SeasonEmployee seasonEmployee) {
        return SeasonEmployeeResponse.builder()
                .id(seasonEmployee.getId())
                .seasonId(seasonEmployee.getSeason() != null ? seasonEmployee.getSeason().getId() : null)
                .seasonName(seasonEmployee.getSeason() != null ? seasonEmployee.getSeason().getSeasonName() : null)
                .employeeUserId(seasonEmployee.getEmployee() != null ? seasonEmployee.getEmployee().getId() : null)
                .employeeUsername(seasonEmployee.getEmployee() != null ? seasonEmployee.getEmployee().getUsername() : null)
                .employeeName(seasonEmployee.getEmployee() != null ? seasonEmployee.getEmployee().getFullName() : null)
                .employeeEmail(seasonEmployee.getEmployee() != null ? seasonEmployee.getEmployee().getEmail() : null)
                .wagePerTask(seasonEmployee.getWagePerTask())
                .active(seasonEmployee.getActive())
                .createdAt(seasonEmployee.getCreatedAt())
                .build();
    }

    private TaskProgressLogResponse toTaskProgressLogResponse(TaskProgressLog logEntry) {
        Task task = logEntry.getTask();
        Season season = task != null ? task.getSeason() : null;
        return TaskProgressLogResponse.builder()
                .id(logEntry.getId())
                .taskId(task != null ? task.getId() : null)
                .taskTitle(task != null ? task.getTitle() : null)
                .seasonId(season != null ? season.getId() : null)
                .seasonName(season != null ? season.getSeasonName() : null)
                .employeeUserId(logEntry.getEmployee() != null ? logEntry.getEmployee().getId() : null)
                .employeeName(logEntry.getEmployee() != null ? logEntry.getEmployee().getFullName() : null)
                .progressPercent(logEntry.getProgressPercent())
                .note(logEntry.getNote())
                .evidenceUrl(logEntry.getEvidenceUrl())
                .loggedAt(logEntry.getLoggedAt())
                .build();
    }

    private PayrollRecordResponse toPayrollRecordResponse(PayrollRecord record) {
        return PayrollRecordResponse.builder()
                .id(record.getId())
                .seasonId(record.getSeason() != null ? record.getSeason().getId() : null)
                .seasonName(record.getSeason() != null ? record.getSeason().getSeasonName() : null)
                .employeeUserId(record.getEmployee() != null ? record.getEmployee().getId() : null)
                .employeeName(record.getEmployee() != null ? record.getEmployee().getFullName() : null)
                .periodStart(record.getPeriodStart())
                .periodEnd(record.getPeriodEnd())
                .totalAssignedTasks(record.getTotalAssignedTasks())
                .totalCompletedTasks(record.getTotalCompletedTasks())
                .wagePerTask(record.getWagePerTask())
                .totalAmount(record.getTotalAmount())
                .generatedAt(record.getGeneratedAt())
                .note(record.getNote())
                .build();
    }

    private TaskResponse toTaskResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId())
                .userName(task.getUser() != null ? task.getUser().getUsername() : null)
                .userId(task.getUser() != null ? task.getUser().getId() : null)
                .seasonId(task.getSeason() != null ? task.getSeason().getId() : null)
                .seasonName(task.getSeason() != null ? task.getSeason().getSeasonName() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .plannedDate(task.getPlannedDate())
                .dueDate(task.getDueDate())
                .status(task.getStatus() != null ? task.getStatus().getCode() : null)
                .actualStartDate(task.getActualStartDate())
                .actualEndDate(task.getActualEndDate())
                .notes(task.getNotes())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
