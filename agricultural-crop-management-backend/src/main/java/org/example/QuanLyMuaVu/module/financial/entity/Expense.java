package org.example.QuanLyMuaVu.module.financial.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Expense entity representing farm expenses.
 * 
 * Extended per Demo Gen Code.docx BR to include:
 * - category: expense category classification
 * - task: optional link to a task
 * - note: optional notes
 * - amount: direct amount field (legacy: unitPrice * quantity = totalCost)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    Integer id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User user;

    @Column(name = "season_id", nullable = false)
    Integer seasonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false, insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.season.entity.Season season;

    /**
     * BR: Optional task reference.
     * org.example.QuanLyMuaVu.module.season.entity.Task must belong to the same season if provided.
     */
    @Column(name = "task_id")
    Integer taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.season.entity.Task task;

    /**
     * BR: Expense category (e.g., SEEDS, FERTILIZER, LABOR, EQUIPMENT, OTHER).
     */
    @Column(name = "category")
    String category;

    // ═══════════════════════════════════════════════════════════════
    // LEGACY FIELDS (kept for backward compatibility)
    // ═══════════════════════════════════════════════════════════════

    @Column(name = "item_name", nullable = false)
    String itemName;

    @Column(name = "unit_price", nullable = false)
    BigDecimal unitPrice;

    @Column(nullable = false)
    Integer quantity;

    @Column(name = "total_cost")
    BigDecimal totalCost;

    // ═══════════════════════════════════════════════════════════════
    // NEW FIELDS (per BR specification)
    // ═══════════════════════════════════════════════════════════════

    /**
     * BR: Direct amount field for simpler expense entries.
     * If not set, totalCost = unitPrice * quantity is used.
     */
    @Column(name = "amount")
    BigDecimal amount;

    @Column(name = "payment_status", length = 30)
    String paymentStatus;

    /**
     * BR: Optional note for the expense.
     */
    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "expense_date", nullable = false)
    LocalDate expenseDate;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    /**
     * Get effective amount (uses amount if set, otherwise totalCost or calculated).
     */
    public BigDecimal getEffectiveAmount() {
        if (amount != null) {
            return amount;
        }
        if (totalCost != null) {
            return totalCost;
        }
        if (unitPrice != null && quantity != null) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
}
