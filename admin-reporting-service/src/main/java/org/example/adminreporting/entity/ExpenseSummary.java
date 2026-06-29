package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "admin_expense_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSummary {
    @Id
    @Column(name = "expense_id")
    private Integer expenseId;

    @Column(name = "season_id", nullable = false)
    private Integer seasonId;

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "category")
    private String category;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
}
