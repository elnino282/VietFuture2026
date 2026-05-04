package org.example.QuanLyMuaVu.module.financial.port;

import java.math.BigDecimal;
import java.util.List;
import org.example.QuanLyMuaVu.module.financial.entity.Expense;

public interface ExpenseQueryPort {

    BigDecimal sumTotalCostBySeasonId(Integer seasonId);

    boolean existsExpenseBySeasonId(Integer seasonId);

    List<ExpenseFertilizerSnapshot> findFertilizerExpensesBySeasonId(Integer seasonId);

    List<Expense> findAllExpenseEntitiesBySeasonIds(Iterable<Integer> seasonIds);

    List<ExpenseSnapshot> findAllExpensesBySeasonIds(Iterable<Integer> seasonIds);

    record ExpenseSnapshot(
            Integer id,
            Integer seasonId,
            Integer taskId,
            String category,
            String itemName,
            BigDecimal totalCost,
            java.time.LocalDate expenseDate) {
    }

    record ExpenseFertilizerSnapshot(
            Integer quantity,
            String itemName,
            String note) {
    }
}
