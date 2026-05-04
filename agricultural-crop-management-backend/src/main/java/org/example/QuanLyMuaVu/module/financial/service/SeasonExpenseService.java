package org.example.QuanLyMuaVu.module.financial.service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.DomainEventPublisher;
import org.example.QuanLyMuaVu.module.shared.pattern.Observer.ExpenseChangedEvent;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.financial.dto.request.CreateExpenseRequest;
import org.example.QuanLyMuaVu.module.financial.dto.request.ExpenseSearchCriteria;
import org.example.QuanLyMuaVu.module.financial.dto.request.UpdateExpenseRequest;
import org.example.QuanLyMuaVu.module.financial.dto.response.ExpenseResponse;
import org.example.QuanLyMuaVu.module.financial.entity.Expense;
import org.example.QuanLyMuaVu.module.financial.repository.ExpenseRepository;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.season.port.TaskQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR174-BR187: org.example.QuanLyMuaVu.module.season.entity.Season Expense Service
 * Handles all expense CRUD operations with BR-compliant validations.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonExpenseService {

    ExpenseRepository expenseRepository;
    SeasonQueryPort seasonQueryPort;
    TaskQueryPort taskQueryPort;
    FarmAccessPort farmAccessService;
    DomainEventPublisher domainEventPublisher;

    // ═══════════════════════════════════════════════════════════════════════════
    // BR176: CreateExpense(Expense expense) - Create Expense with Full Validation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR176: CreateExpense method with parameter "expense" as an object class.
     * Validates all constraints before saving to database.
     *
     * @param seasonId the season ID from path variable [cmbSeasonID]
     * @param request  the expense creation request containing all form fields
     * @return ExpenseResponse on success (MSG 7)
     * @throws AppException with MSG 9 on constraint violation
     */
    public ExpenseResponse CreateExpense(Integer seasonId, CreateExpenseRequest request) {
        // BR175: ValidateDataFormat() - Input validation done via @Valid annotations

        // BR176: Validate season exists and farmer has access
        org.example.QuanLyMuaVu.module.season.entity.Season season = getSeasonForCurrentFarmer(seasonId);

        // BR176: Validate season is not closed/archived
        ensureSeasonOpenForExpenses(season);

        // BR176: Validate [Expense.season_id] is consistent with [Expense.plot_id]
        validateSeasonBelongsToPlot(season, request.getPlotId());

        // BR176: Validate [Expense.task_id] belongs to org.example.QuanLyMuaVu.module.season.entity.Season/Plot if provided
        org.example.QuanLyMuaVu.module.season.entity.Task task = null;
        if (request.getTaskId() != null) {
            task = validateTaskBelongsToSeason(request.getTaskId(), seasonId);
        }

        // BR175: Validate amount > 0
        validateAmount(request.getAmount());

        // Validate expense date within season dates
        validateExpenseDateWithinSeason(season, request.getExpenseDate());

        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();

        // Calculate totalCost for legacy compatibility
        BigDecimal totalCost = request.getAmount();
        if (request.getUnitPrice() != null && request.getQuantity() != null) {
            totalCost = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        }

        // Determine itemName (use category if not provided)
        String itemName = request.getItemName();
        if (itemName == null || itemName.isBlank()) {
            itemName = request.getCategory() != null ? request.getCategory() : "Expense";
        }

        Expense expense = Expense.builder()
                .userId(currentUser.getId())
                .user(currentUser)
                .seasonId(season.getId())
                .season(season)
                .taskId(task != null ? task.getId() : null)
                .task(task)
                .category(request.getCategory())
                .amount(request.getAmount())
                .note(request.getNote())
                .itemName(itemName)
                .unitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : request.getAmount())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .totalCost(totalCost)
                .expenseDate(request.getExpenseDate())
                .createdAt(LocalDateTime.now())
                .build();

        Expense saved = expenseRepository.save(expense);
        domainEventPublisher.publish(new ExpenseChangedEvent(saved, ExpenseChangedEvent.Action.CREATED));

        // BR176: Step (7) - Return success (MSG 7 handled by controller)
        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR177: Query expense by ID - SELECT * FROM expense WHERE expense_id = [ID]
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR177: Query expense by ID for display on update screen.
     * Used by displayExpenseUpdateScreen(Expense expense) method.
     *
     * @param id the expense ID from datagrid selection
     * @return ExpenseResponse with all fields for BR178 display
     * @throws AppException with MSG 10 if not found
     */
    public ExpenseResponse getExpense(Integer id) {
        Expense expense = getExpenseForCurrentFarmer(id);
        return toResponse(expense);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR180: UpdateExpense(Expense expense) - Update Expense with Full Validation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR180: UpdateExpense method with parameter "expense" as an object class.
     * Validates all constraints before updating in database.
     *
     * @param id      the expense ID to update
     * @param request the expense update request containing all form fields
     * @return ExpenseResponse on success (MSG 7)
     * @throws AppException with MSG 9 on constraint violation
     */
    public ExpenseResponse UpdateExpense(Integer id, UpdateExpenseRequest request) {
        // BR179: ValidateDataFormat() - Input validation done via @Valid annotations

        Expense expense = getExpenseForCurrentFarmer(id);

        // Get target season (may be different if user changed season)
        org.example.QuanLyMuaVu.module.season.entity.Season targetSeason = getSeasonForCurrentFarmer(request.getSeasonId());

        // BR180: Validate target season is not closed/archived
        ensureSeasonOpenForExpenses(targetSeason);

        // BR180: Validate [Expense.season_id] is consistent with [Expense.plot_id]
        validateSeasonBelongsToPlot(targetSeason, request.getPlotId());

        // BR180: Validate [Expense.task_id] belongs to org.example.QuanLyMuaVu.module.season.entity.Season/Plot if provided
        org.example.QuanLyMuaVu.module.season.entity.Task task = null;
        if (request.getTaskId() != null) {
            task = validateTaskBelongsToSeason(request.getTaskId(), request.getSeasonId());
        }

        // BR179: Validate amount > 0
        validateAmount(request.getAmount());

        // Validate expense date within season dates
        validateExpenseDateWithinSeason(targetSeason, request.getExpenseDate());

        // Calculate totalCost for legacy compatibility
        BigDecimal totalCost = request.getAmount();
        if (request.getUnitPrice() != null && request.getQuantity() != null) {
            totalCost = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        }

        // Determine itemName (use category if not provided)
        String itemName = request.getItemName();
        if (itemName == null || itemName.isBlank()) {
            itemName = request.getCategory() != null ? request.getCategory() : expense.getItemName();
        }

        // Update all fields
        expense.setSeasonId(targetSeason.getId());
        expense.setSeason(targetSeason);
        expense.setTaskId(task != null ? task.getId() : null);
        expense.setTask(task);
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setNote(request.getNote());
        expense.setItemName(itemName);
        expense.setUnitPrice(request.getUnitPrice() != null ? request.getUnitPrice() : request.getAmount());
        expense.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        expense.setTotalCost(totalCost);
        expense.setExpenseDate(request.getExpenseDate());

        Expense saved = expenseRepository.save(expense);
        domainEventPublisher.publish(new ExpenseChangedEvent(saved, ExpenseChangedEvent.Action.UPDATED));

        // BR180: Step (8) - Return success (MSG 7 handled by controller)
        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR182/BR183: DeleteExpense(Expense expense) - Delete with Constraint Check
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR182: DeleteExpense method called after confirmation dialog.
     * BR183: Validates constraints before deleting from database.
     *
     * @param id the expense ID to delete
     * @throws AppException with MSG 9 on constraint violation
     */
    public void DeleteExpense(Integer id) {
        Expense expense = getExpenseForCurrentFarmer(id);

        // BR183: Check constraints - season must not be closed
        ensureSeasonOpenForExpenses(resolveSeason(expense));

        expenseRepository.delete(expense);
        domainEventPublisher.publish(new ExpenseChangedEvent(expense, ExpenseChangedEvent.Action.DELETED));
        // BR183: Step (7) - Success (MSG 7 handled by controller)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR185: SearchExpense(ExpenseSearchCriteria criteria) - Search with Criteria
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR185: SearchExpense method using ExpenseSearchCriteria.
     * Queries expense table based on given parameters.
     *
     * @param criteria the search criteria from form controls
     * @param page     page number (0-indexed)
     * @param size     page size
     * @return PageResponse with results (BR186) or empty with MSG 10 indicator
     *         (BR187)
     */
    public PageResponse<ExpenseResponse> SearchExpense(ExpenseSearchCriteria criteria, int page, int size) {
        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();

        // Get all expenses for this farmer's seasons
        List<org.example.QuanLyMuaVu.module.season.entity.Season> farmerSeasons = seasonQueryPort.findAllSeasonsByOwnerId(currentUser.getId());
        List<Integer> seasonIds = farmerSeasons.stream().map(org.example.QuanLyMuaVu.module.season.entity.Season::getId).toList();

        if (seasonIds.isEmpty()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            return PageResponse.of(new PageImpl<>(List.of(), pageable, 0), List.of());
        }

        // Get all expenses for these seasons
        List<Expense> allExpenses = new ArrayList<>();
        for (Integer seasonId : seasonIds) {
            allExpenses.addAll(expenseRepository.findAllBySeasonId(seasonId));
        }

        // BR185: Apply search criteria filters
        List<ExpenseResponse> filtered = allExpenses.stream()
                .filter(expense -> {
                    org.example.QuanLyMuaVu.module.season.entity.Season expenseSeason = resolveSeason(expense);
                    // Filter by seasonId
                    if (criteria.getSeasonId() != null &&
                            !criteria.getSeasonId().equals(expense.getSeasonId())) {
                        return false;
                    }
                    // Filter by plotId (season's plot)
                    if (criteria.getPlotId() != null &&
                            (expenseSeason == null || expenseSeason.getPlot() == null ||
                                    !criteria.getPlotId().equals(expenseSeason.getPlot().getId()))) {
                        return false;
                    }
                    // Filter by taskId
                    if (criteria.getTaskId() != null &&
                            !criteria.getTaskId().equals(expense.getTaskId())) {
                        return false;
                    }
                    // Filter by category
                    if (criteria.getCategory() != null && !criteria.getCategory().isBlank() &&
                            (expense.getCategory() == null ||
                                    !expense.getCategory().equalsIgnoreCase(criteria.getCategory()))) {
                        return false;
                    }
                    // Filter by date range
                    LocalDate date = expense.getExpenseDate();
                    if (criteria.getFromDate() != null && date.isBefore(criteria.getFromDate())) {
                        return false;
                    }
                    if (criteria.getToDate() != null && date.isAfter(criteria.getToDate())) {
                        return false;
                    }
                    // Filter by amount range
                    BigDecimal amount = expense.getEffectiveAmount();
                    if (criteria.getMinAmount() != null && amount.compareTo(criteria.getMinAmount()) < 0) {
                        return false;
                    }
                    if (criteria.getMaxAmount() != null && amount.compareTo(criteria.getMaxAmount()) > 0) {
                        return false;
                    }
                    // Filter by keyword (itemName)
                    if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                        String kw = criteria.getKeyword().toLowerCase();
                        if (expense.getItemName() == null ||
                                !expense.getItemName().toLowerCase().contains(kw)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((e1, e2) -> Integer.compare(
                        e2.getId() != null ? e2.getId() : 0,
                        e1.getId() != null ? e1.getId() : 0))
                .map(this::toResponse)
                .toList();

        // Paginate
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<ExpenseResponse> pageItems = fromIndex >= filtered.size() ? List.of()
                : filtered.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ExpenseResponse> pageData = new PageImpl<>(pageItems, pageable, filtered.size());

        // BR186: Return results if available
        // BR187: Empty results indicate "Expense not found" (MSG 10 handled by
        // controller)
        return PageResponse.of(pageData, pageItems);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ADDITIONAL LIST METHODS (For compatibility with existing functionality)
    // ═══════════════════════════════════════════════════════════════════════════

    public PageResponse<ExpenseResponse> listExpensesForSeason(
            Integer seasonId,
            LocalDate from,
            LocalDate to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int page,
            int size) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = getSeasonForCurrentFarmer(seasonId);

        List<Expense> all = expenseRepository.findAllBySeasonId(season.getId());

        List<ExpenseResponse> items = all.stream()
                .filter(expense -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    LocalDate date = expense.getExpenseDate();
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .filter(expense -> {
                    BigDecimal total = expense.getEffectiveAmount();
                    boolean aboveMin = minAmount == null || total.compareTo(minAmount) >= 0;
                    boolean belowMax = maxAmount == null || total.compareTo(maxAmount) <= 0;
                    return aboveMin && belowMax;
                })
                .sorted((e1, e2) -> Integer.compare(
                        e2.getId() != null ? e2.getId() : 0,
                        e1.getId() != null ? e1.getId() : 0))
                .map(this::toResponse)
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, items.size());
        List<ExpenseResponse> pageItems = fromIndex >= items.size() ? List.of() : items.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ExpenseResponse> pageData = new PageImpl<>(pageItems, pageable, items.size());

        return PageResponse.of(pageData, pageItems);
    }

    public PageResponse<ExpenseResponse> listAllFarmerExpenses(
            Integer seasonId,
            String q,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {

        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = getCurrentUser();
        Long userId = currentUser.getId();

        List<Expense> all;
        if (seasonId != null) {
            org.example.QuanLyMuaVu.module.season.entity.Season season = getSeasonForCurrentFarmer(seasonId);
            all = expenseRepository.findAllBySeasonId(season.getId());
        } else if (q != null && !q.trim().isEmpty()) {
            all = expenseRepository.findAllByUserIdAndItemNameContainingIgnoreCaseOrderByExpenseDateDesc(userId,
                    q.trim());
        } else {
            all = expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId);
        }

        List<ExpenseResponse> items = all.stream()
                .filter(expense -> {
                    if (from == null && to == null) {
                        return true;
                    }
                    LocalDate date = expense.getExpenseDate();
                    boolean afterFrom = from == null || !date.isBefore(from);
                    boolean beforeTo = to == null || !date.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .filter(expense -> {
                    if (seasonId == null || q == null || q.trim().isEmpty()) {
                        return true;
                    }
                    return expense.getItemName().toLowerCase().contains(q.toLowerCase().trim());
                })
                .sorted((e1, e2) -> {
                    int dateCompare = e2.getExpenseDate().compareTo(e1.getExpenseDate());
                    if (dateCompare != 0)
                        return dateCompare;
                    return Integer.compare(
                            e2.getId() != null ? e2.getId() : 0,
                            e1.getId() != null ? e1.getId() : 0);
                })
                .map(this::toResponse)
                .toList();

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, items.size());
        List<ExpenseResponse> pageItems = fromIndex >= items.size() ? List.of() : items.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("expenseDate").descending());
        Page<ExpenseResponse> pageData = new PageImpl<>(pageItems, pageable, items.size());

        return PageResponse.of(pageData, pageItems);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LEGACY WRAPPER METHODS (For backward compatibility)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Legacy createExpense method for backward compatibility.
     * Delegates to the new BR-compliant CreateExpense method.
     */
    public ExpenseResponse createExpense(Integer seasonId, CreateExpenseRequest request) {
        return CreateExpense(seasonId, request);
    }

    /**
     * Legacy updateExpense method for backward compatibility.
     */
    public ExpenseResponse updateExpense(Integer id, UpdateExpenseRequest request) {
        return UpdateExpense(id, request);
    }

    /**
     * Legacy deleteExpense method for backward compatibility.
     */
    public void deleteExpense(Integer id) {
        DeleteExpense(id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BR176/BR180: CONSTRAINT VALIDATION METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * BR176/BR180: Validate that season belongs to the specified plot.
     * If not, throw MSG 9: "Your action is failed due to constraints in the
     * system."
     *
     * @param season the season to validate
     * @param plotId the expected plot ID
     * @throws AppException with MSG_9_CONSTRAINT_VIOLATION if mismatch
     */
    private void validateSeasonBelongsToPlot(org.example.QuanLyMuaVu.module.season.entity.Season season, Integer plotId) {
        if (plotId == null) {
            throw new AppException(ErrorCode.MSG_1_MANDATORY_FIELD_EMPTY);
        }
        if (season.getPlot() == null || !season.getPlot().getId().equals(plotId)) {
            // MSG 9: "Your action is failed due to constraints in the system."
            throw new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION);
        }
    }

    /**
     * BR176/BR180: Validate that task belongs to the specified season.
     * If not, throw MSG 9: "Your action is failed due to constraints in the
     * system."
     *
     * @param taskId   the task ID to validate
     * @param seasonId the expected season ID
     * @return the validated org.example.QuanLyMuaVu.module.season.entity.Task entity
     * @throws AppException with MSG_9_CONSTRAINT_VIOLATION if mismatch
     */
    private org.example.QuanLyMuaVu.module.season.entity.Task validateTaskBelongsToSeason(Integer taskId, Integer seasonId) {
        if (!taskQueryPort.existsTaskByIdAndSeasonId(taskId, seasonId)) {
            // MSG 9: "Your action is failed due to constraints in the system."
            throw new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION);
        }
        return taskQueryPort.findTaskByIdAndSeasonId(taskId, seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION));
    }

    /**
     * BR175/BR179: Validate that amount is greater than 0.
     *
     * @param amount the amount to validate
     * @throws AppException with MSG_4_INVALID_FORMAT if amount <= 0
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            // MSG 4: "Invalid data format. Please enter again."
            throw new AppException(ErrorCode.MSG_4_INVALID_FORMAT);
        }
    }

    /**
     * Ensure season is open for expense modifications.
     * BR176/BR180: Cannot modify expenses in closed/archived seasons.
     */
    private void ensureSeasonOpenForExpenses(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            throw new AppException(ErrorCode.EXPENSE_PERIOD_LOCKED);
        }
    }

    /**
     * Validate expense date is within season date range.
     */
    private void validateExpenseDateWithinSeason(org.example.QuanLyMuaVu.module.season.entity.Season season, LocalDate date) {
        LocalDate start = season.getStartDate();
        LocalDate end = season.getEndDate() != null ? season.getEndDate() : season.getPlannedHarvestDate();

        if (start == null || date.isBefore(start) || (end != null && date.isAfter(end))) {
            throw new AppException(ErrorCode.INVALID_SEASON_DATES);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private Expense getExpenseForCurrentFarmer(Integer id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MSG_10_EXPENSE_NOT_FOUND));

        org.example.QuanLyMuaVu.module.season.entity.Season season = resolveSeason(expense);
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return expense;
    }

    private org.example.QuanLyMuaVu.module.season.entity.Season getSeasonForCurrentFarmer(Integer id) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = seasonQueryPort.findSeasonById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MSG_9_CONSTRAINT_VIOLATION));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private org.example.QuanLyMuaVu.module.identity.entity.User getCurrentUser() {
        return farmAccessService.getCurrentUser();
    }

    private org.example.QuanLyMuaVu.module.season.entity.Season resolveSeason(Expense expense) {
        if (expense == null) {
            return null;
        }
        if (expense.getSeason() != null) {
            return expense.getSeason();
        }
        Integer seasonId = expense.getSeasonId();
        if (seasonId == null) {
            return null;
        }
        return seasonQueryPort.findSeasonById(seasonId).orElse(null);
    }

    /**
     * Convert Expense entity to ExpenseResponse DTO.
     * Includes all fields required by BR177/BR178 for display screens.
     */
    private ExpenseResponse toResponse(Expense expense) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = resolveSeason(expense);
        org.example.QuanLyMuaVu.module.season.entity.Task task = expense.getTask();

        return ExpenseResponse.builder()
                .id(expense.getId())
                // org.example.QuanLyMuaVu.module.season.entity.Season info
                .seasonId(expense.getSeasonId() != null ? expense.getSeasonId() : season != null ? season.getId() : null)
                .seasonName(season != null ? season.getSeasonName() : null)
                // Plot info (BR176/BR180)
                .plotId(season != null && season.getPlot() != null
                        ? season.getPlot().getId()
                        : null)
                .plotName(season != null && season.getPlot() != null
                        ? season.getPlot().getPlotName()
                        : null)
                // org.example.QuanLyMuaVu.module.season.entity.Task info (BR176/BR180)
                .taskId(expense.getTaskId() != null ? expense.getTaskId() : task != null ? task.getId() : null)
                .taskTitle(task != null ? task.getTitle() : null)
                // org.example.QuanLyMuaVu.module.identity.entity.User info
                .userName(expense.getUser() != null ? expense.getUser().getUsername() : null)
                // BR fields
                .category(expense.getCategory())
                .amount(expense.getEffectiveAmount())
                .note(expense.getNote())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                // Legacy fields
                .itemName(expense.getItemName())
                .unitPrice(expense.getUnitPrice())
                .quantity(expense.getQuantity())
                .totalCost(expense.getTotalCost())
                .build();
    }
}
