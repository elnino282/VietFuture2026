package org.example.QuanLyMuaVu.module.financial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * BR177/BR178: Expense Response DTO.
 * Contains all fields required by the Expense display/update screens.
 * Used by displayExpenseUpdateScreen(Expense expense) method.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseResponse {

    /**
     * Expense unique identifier.
     */
    Integer id;

    /**
     * BR177: Season ID for the expense.
     */
    Integer seasonId;

    /**
     * Season name for display.
     */
    String seasonName;

    /**
     * BR176/BR180: Plot ID for season-plot validation.
     */
    Integer plotId;

    /**
     * Plot name for display.
     */
    String plotName;

    /**
     * BR176/BR180: Task ID if expense is linked to a task.
     */
    Integer taskId;

    /**
     * Task title for display.
     */
    String taskTitle;

    /**
     * Username of expense creator.
     */
    String userName;

    /**
     * BR175/BR179: Expense category.
     */
    String category;

    /**
     * BR175/BR179: Expense amount.
     */
    BigDecimal amount;

    /**
     * BR175/BR179: Expense note/description.
     */
    String note;

    /**
     * BR175/BR179: Expense date.
     */
    LocalDate expenseDate;

    /**
     * Created timestamp.
     */
    LocalDateTime createdAt;

    // ═══════════════════════════════════════════════════════════════
    // LEGACY FIELDS (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Legacy: Item name/description.
     */
    String itemName;

    /**
     * Legacy: Unit price.
     */
    BigDecimal unitPrice;

    /**
     * Legacy: Quantity.
     */
    Integer quantity;

    /**
     * Legacy: Total cost (unitPrice * quantity).
     */
    BigDecimal totalCost;
}
