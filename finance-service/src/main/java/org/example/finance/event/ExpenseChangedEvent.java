package org.example.finance.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import org.example.finance.entity.Expense;

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
    private final Long userId;
    private final Integer plotId;
    private final Integer farmId;
    private final String category;
    private final String itemName;
    private final BigDecimal unitPrice;
    private final Integer quantity;
    private final BigDecimal totalCost;
    private final BigDecimal amount;
    private final String paymentStatus;
    private final String note;
    private final LocalDate expenseDate;
    private final String seasonName;
    private final String plotName;
    private final String taskTitle;
    private final String userName;

    public ExpenseChangedEvent(Expense expense, Action action, Long ownerUserId) {
        super("Expense",
              expense != null && expense.getId() != null ? expense.getId().toString() : "unknown",
              "finance-service",
              "finance.event.expense." + action.name().toLowerCase());
        this.action = action;
        this.expenseId = expense != null ? expense.getId() : null;
        this.seasonId = expense != null ? expense.getSeasonId() : null;
        this.taskId = expense != null ? expense.getTaskId() : null;
        this.ownerUserId = ownerUserId;
        this.userId = expense != null ? expense.getUserId() : null;
        this.plotId = expense != null ? expense.getPlotId() : null;
        this.farmId = expense != null ? expense.getFarmId() : null;
        this.category = expense != null ? expense.getCategory() : null;
        this.itemName = expense != null ? expense.getItemName() : null;
        this.unitPrice = expense != null ? expense.getUnitPrice() : null;
        this.quantity = expense != null ? expense.getQuantity() : null;
        this.totalCost = expense != null ? expense.getTotalCost() : null;
        this.amount = expense != null ? expense.getEffectiveAmount() : null;
        this.paymentStatus = expense != null ? expense.getPaymentStatus() : null;
        this.note = expense != null ? expense.getNote() : null;
        this.expenseDate = expense != null ? expense.getExpenseDate() : null;
        this.seasonName = expense != null ? expense.getSeasonName() : null;
        this.plotName = expense != null ? expense.getPlotName() : null;
        this.taskTitle = expense != null ? expense.getTaskTitle() : null;
        this.userName = expense != null ? expense.getUserName() : null;
    }
}
