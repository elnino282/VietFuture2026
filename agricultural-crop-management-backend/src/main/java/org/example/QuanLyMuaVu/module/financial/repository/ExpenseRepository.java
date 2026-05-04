package org.example.QuanLyMuaVu.module.financial.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.example.QuanLyMuaVu.module.admin.dto.response.AdminReportProjections;
import org.example.QuanLyMuaVu.module.financial.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    List<Expense> findByItemNameContainingIgnoreCase(String itemName);

    List<Expense> findAllBySeasonId(Integer seasonId);

    List<Expense> findAllBySeason_Id(Integer seasonId);

    List<Expense> findAllBySeasonIdIn(Iterable<Integer> seasonIds);

    List<Expense> findAllBySeason_IdIn(Iterable<Integer> seasonIds);

    List<Expense> findAllBySeasonIdAndCategoryIgnoreCase(Integer seasonId, String category);

    List<Expense> findAllBySeason_IdAndCategoryIgnoreCase(Integer seasonId, String category);

    List<Expense> findAllBySeasonIdAndExpenseDateBetween(Integer seasonId, LocalDate from, LocalDate to);

    List<Expense> findAllBySeason_IdAndExpenseDateBetween(Integer seasonId, LocalDate from, LocalDate to);

    boolean existsBySeasonId(Integer seasonId);

    boolean existsBySeason_Id(Integer seasonId);

    // Methods for fetching all farmer's expenses
    List<Expense> findAllByUserIdOrderByExpenseDateDesc(Long userId);

    List<Expense> findAllByUser_IdOrderByExpenseDateDesc(Long userId);

    List<Expense> findAllByUserIdAndSeasonIdOrderByExpenseDateDesc(Long userId, Integer seasonId);

    List<Expense> findAllByUser_IdAndSeason_IdOrderByExpenseDateDesc(Long userId, Integer seasonId);

    List<Expense> findAllByUserIdAndItemNameContainingIgnoreCaseOrderByExpenseDateDesc(Long userId, String itemName);

    List<Expense> findAllByUser_IdAndItemNameContainingIgnoreCaseOrderByExpenseDateDesc(Long userId, String itemName);

    /**
     * Sum total expenses for a season.
     * Used for dashboard expense totals.
     */
    @Query("SELECT COALESCE(SUM(e.totalCost), 0) FROM Expense e WHERE e.seasonId = :seasonId")
    BigDecimal sumTotalCostBySeasonId(@Param("seasonId") Integer seasonId);

    // ═══════════════════════════════════════════════════════════════
    // ADMIN AGGREGATION METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sum expenses for date range (admin dashboard).
     */
    @Query("SELECT COALESCE(SUM(e.totalCost), 0) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByExpenseDateBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum expenses grouped by season ID.
     */
    @Query("SELECT e.seasonId AS seasonId, COALESCE(SUM(e.totalCost), 0) AS totalExpense " +
            "FROM Expense e WHERE e.seasonId IN :seasonIds GROUP BY e.seasonId")
    List<AdminReportProjections.SeasonExpenseAgg> sumExpensesBySeasonIds(
            @Param("seasonIds") Set<Integer> seasonIds);
}
