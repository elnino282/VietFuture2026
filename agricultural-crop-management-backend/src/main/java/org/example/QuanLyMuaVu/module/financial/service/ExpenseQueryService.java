package org.example.QuanLyMuaVu.module.financial.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.financial.port.ExpenseQueryPort;
import org.example.QuanLyMuaVu.module.financial.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class ExpenseQueryService implements ExpenseQueryPort {

    ExpenseRepository expenseRepository;

    @Override
    public BigDecimal sumTotalCostBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = expenseRepository.sumTotalCostBySeasonId(seasonId);
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    public boolean existsExpenseBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return false;
        }
        return expenseRepository.existsBySeasonId(seasonId);
    }

    @Override
    public List<ExpenseFertilizerSnapshot> findFertilizerExpensesBySeasonId(Integer seasonId) {
        if (seasonId == null) {
            return List.of();
        }
        return expenseRepository.findAllBySeasonIdAndCategoryIgnoreCase(seasonId, "FERTILIZER")
                .stream()
                .map(expense -> new ExpenseFertilizerSnapshot(
                        expense.getQuantity(),
                        expense.getItemName(),
                        expense.getNote()))
                .toList();
    }

    @Override
    public List<org.example.QuanLyMuaVu.module.financial.entity.Expense> findAllExpenseEntitiesBySeasonIds(
            Iterable<Integer> seasonIds) {
        if (seasonIds == null) {
            return List.of();
        }
        return expenseRepository.findAllBySeasonIdIn(seasonIds);
    }

    @Override
    public List<ExpenseSnapshot> findAllExpensesBySeasonIds(Iterable<Integer> seasonIds) {
        if (seasonIds == null) {
            return List.of();
        }
        return expenseRepository.findAllBySeasonIdIn(seasonIds)
                .stream()
                .map(expense -> new ExpenseSnapshot(
                        expense.getId(),
                        expense.getSeasonId(),
                        expense.getTaskId(),
                        expense.getCategory(),
                        expense.getItemName(),
                        expense.getTotalCost(),
                        expense.getExpenseDate()))
                .toList();
    }
}
