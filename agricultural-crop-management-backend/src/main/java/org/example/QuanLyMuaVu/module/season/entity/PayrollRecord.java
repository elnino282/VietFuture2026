package org.example.QuanLyMuaVu.module.season.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "payroll_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_employee_season_period", columnNames = {
                "employee_user_id", "season_id", "period_start", "period_end"
        })
}, indexes = {
        @Index(name = "idx_payroll_employee", columnList = "employee_user_id"),
        @Index(name = "idx_payroll_season", columnList = "season_id"),
        @Index(name = "idx_payroll_period", columnList = "period_start,period_end")
})
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "employee_user_id", nullable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User employee;

    @ManyToOne
    @JoinColumn(name = "season_id")
    Season season;

    @Column(name = "period_start", nullable = false)
    LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    LocalDate periodEnd;

    @Column(name = "total_assigned_tasks", nullable = false)
    Integer totalAssignedTasks;

    @Column(name = "total_completed_tasks", nullable = false)
    Integer totalCompletedTasks;

    @Column(name = "wage_per_task", precision = 15, scale = 2, nullable = false)
    BigDecimal wagePerTask;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    BigDecimal totalAmount;

    @Column(name = "generated_at", nullable = false)
    LocalDateTime generatedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @PrePersist
    void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}

