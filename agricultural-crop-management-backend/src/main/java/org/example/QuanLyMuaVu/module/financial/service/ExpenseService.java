package org.example.QuanLyMuaVu.module.financial.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.module.financial.dto.request.ExpenseRequest;
import org.example.QuanLyMuaVu.module.financial.dto.response.ExpenseResponse;
import org.example.QuanLyMuaVu.module.financial.entity.Expense;
import org.example.QuanLyMuaVu.module.financial.repository.ExpenseRepository;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private static final String USER_NOT_FOUND_MESSAGE = "User not found";
    private static final String SEASON_NOT_FOUND_MESSAGE = "Season not found";
    private static final String EXPENSE_NOT_FOUND_MESSAGE = "Expense not found";

    private final ExpenseRepository expenseRepository;
    private final IdentityQueryPort identityQueryPort;
    private final SeasonQueryPort seasonQueryPort;

    // ---------------------------
    // CREATE
    // ---------------------------
    public ExpenseResponse createExpense(ExpenseRequest request) {
        org.example.QuanLyMuaVu.module.identity.entity.User user = resolveUser(request.getUserId());
        org.example.QuanLyMuaVu.module.season.entity.Season season = resolveSeason(request.getSeasonId());
        BigDecimal totalCost = calculateTotalCost(request.getUnitPrice(), request.getQuantity());

        Expense expense = Expense.builder()
                .userId(user.getId())
                .user(user)
                .seasonId(season.getId())
                .season(season)
                .itemName(request.getItemName())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .totalCost(totalCost)
                .amount(totalCost)
                .expenseDate(request.getExpenseDate())
                .createdAt(LocalDateTime.now())
                .build();

        return mapToResponse(expenseRepository.save(expense));
    }

    // ---------------------------
    // GET by ID
    // ---------------------------
    public ExpenseResponse getExpenseById(Integer id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(EXPENSE_NOT_FOUND_MESSAGE));
        return mapToResponse(expense);
    }

    // ---------------------------
    // GET ALL
    // ---------------------------
    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---------------------------
    // SEARCH by name
    // ---------------------------
    public List<ExpenseResponse> searchExpensesByName(String name) {
        return expenseRepository.findByItemNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---------------------------
    // UPDATE
    // ---------------------------
    public ExpenseResponse updateExpense(Integer id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(EXPENSE_NOT_FOUND_MESSAGE));

        org.example.QuanLyMuaVu.module.identity.entity.User user = resolveUser(request.getUserId());
        org.example.QuanLyMuaVu.module.season.entity.Season season = resolveSeason(request.getSeasonId());
        BigDecimal totalCost = calculateTotalCost(request.getUnitPrice(), request.getQuantity());

        expense.setUserId(user.getId());
        expense.setUser(user);
        expense.setSeasonId(season.getId());
        expense.setSeason(season);
        expense.setItemName(request.getItemName());
        expense.setUnitPrice(request.getUnitPrice());
        expense.setQuantity(request.getQuantity());
        expense.setTotalCost(totalCost);
        expense.setAmount(totalCost);
        expense.setExpenseDate(request.getExpenseDate());

        return mapToResponse(expenseRepository.save(expense));
    }

    // ---------------------------
    // DELETE
    // ---------------------------
    public void deleteExpense(Integer id) {
        if (!expenseRepository.existsById(id)) {
            throw new RuntimeException(EXPENSE_NOT_FOUND_MESSAGE);
        }
        expenseRepository.deleteById(id);
    }

    private org.example.QuanLyMuaVu.module.identity.entity.User resolveUser(Long userId) {
        return identityQueryPort.findUserById(userId)
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));
    }

    private org.example.QuanLyMuaVu.module.season.entity.Season resolveSeason(Integer seasonId) {
        return seasonQueryPort.findSeasonById(seasonId)
                .orElseThrow(() -> new RuntimeException(SEASON_NOT_FOUND_MESSAGE));
    }

    private BigDecimal calculateTotalCost(BigDecimal unitPrice, Integer quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ---------------------------
    // PRIVATE MAPPER
    // ---------------------------
    private ExpenseResponse mapToResponse(Expense expense) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = expense.getSeason();
        org.example.QuanLyMuaVu.module.season.entity.Task task = expense.getTask();

        return ExpenseResponse.builder()
                .id(expense.getId())
                .seasonId(expense.getSeasonId())
                .seasonName(season != null ? season.getSeasonName() : null)
                .plotId(season != null && season.getPlot() != null ? season.getPlot().getId() : null)
                .plotName(season != null && season.getPlot() != null ? season.getPlot().getPlotName() : null)
                .taskId(expense.getTaskId() != null ? expense.getTaskId() : task != null ? task.getId() : null)
                .taskTitle(task != null ? task.getTitle() : null)
                .userName(expense.getUser() != null ? expense.getUser().getUsername() : null)
                .category(expense.getCategory())
                .amount(expense.getEffectiveAmount())
                .note(expense.getNote())
                .itemName(expense.getItemName())
                .unitPrice(expense.getUnitPrice())
                .quantity(expense.getQuantity())
                .totalCost(expense.getTotalCost())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
