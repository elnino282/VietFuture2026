package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class ExpenseChangedEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }

    private final Action action;
    private final Integer expenseId;
    private final Integer seasonId;
    private final Integer taskId;
    private final Long ownerUserId;
    private final String category;
    private final BigDecimal amount;

    public ExpenseChangedEvent(org.example.QuanLyMuaVu.module.financial.entity.Expense expense, Action action) {
        super("org.example.QuanLyMuaVu.module.financial.entity.Expense", expense != null && expense.getId() != null ? expense.getId().toString() : "unknown");
        this.action = action;
        this.expenseId = expense != null ? expense.getId() : null;
        this.seasonId = expense != null
                ? expense.getSeasonId() != null ? expense.getSeasonId()
                        : expense.getSeason() != null ? expense.getSeason().getId() : null
                : null;
        this.taskId = expense != null
                ? expense.getTaskId() != null ? expense.getTaskId()
                        : expense.getTask() != null ? expense.getTask().getId() : null
                : null;
        this.ownerUserId = expense != null
                && expense.getSeason() != null
                && expense.getSeason().getPlot() != null
                && expense.getSeason().getPlot().getFarm() != null
                && expense.getSeason().getPlot().getFarm().getUser() != null
                        ? expense.getSeason().getPlot().getFarm().getUser().getId()
                        : null;
        this.category = expense != null ? expense.getCategory() : null;
        this.amount = expense != null ? expense.getEffectiveAmount() : null;
    }

    @Override
    public String getEventType() {
        return "EXPENSE_" + action.name();
    }
}
