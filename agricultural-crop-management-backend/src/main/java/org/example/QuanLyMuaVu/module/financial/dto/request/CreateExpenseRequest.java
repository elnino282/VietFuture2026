package org.example.QuanLyMuaVu.module.financial.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * BR174-BR176: Create Expense Request DTO.
 * Contains all fields required by the Expense Creation screen as per Business
 * Rules.
 * 
 * Mandatory fields (BR175):
 * - amount: Expense amount (must be > 0)
 * - expenseDate: Date of expense
 * - category: Expense category
 * - seasonId: Season ID (inherited from path variable)
 * - plotId: Plot ID (for season-plot validation)
 * 
 * Optional fields:
 * - taskId: Link to specific task (validated against season)
 * - note: Expense description
 * - itemName: Legacy field - item description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateExpenseRequest {

    /**
     * BR175: [txtBoxAmount] - Expense amount (mandatory).
     * Must be greater than 0.
     */
    @NotNull(message = "MSG_1")
    @DecimalMin(value = "0.0", inclusive = false, message = "MSG_4")
    BigDecimal amount;

    /**
     * BR175: [txtBoxExpenseDate] - Expense date (mandatory).
     * Must be a valid date format.
     */
    @NotNull(message = "MSG_1")
    LocalDate expenseDate;

    /**
     * BR175: [cmbCategory] - Expense category (mandatory).
     * E.g., SEEDS, FERTILIZER, LABOR, EQUIPMENT, OTHER.
     */
    @NotBlank(message = "MSG_1")
    @Size(max = 50, message = "MSG_4")
    String category;

    /**
     * BR175: [cmbPlotID] - Plot ID for validation (mandatory).
     * Used to verify season belongs to this plot.
     */
    @NotNull(message = "MSG_1")
    Integer plotId;

    /**
     * BR175: [cmbTaskID] - Task ID (optional).
     * If provided, must belong to the selected Season/Plot.
     */
    Integer taskId;

    /**
     * BR175: [txtBoxNote] - Expense note (optional).
     */
    @Size(max = 1000, message = "MSG_4")
    String note;

    // ═══════════════════════════════════════════════════════════════
    // LEGACY FIELDS (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Legacy: Item name/description.
     * If not provided, defaults to category or "Expense".
     */
    @Size(max = 255, message = "MSG_4")
    String itemName;

    /**
     * Legacy: Unit price (optional, for itemized expenses).
     */
    BigDecimal unitPrice;

    /**
     * Legacy: Quantity (optional, for itemized expenses).
     */
    Integer quantity;
}
