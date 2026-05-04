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
@Table(name = "season_employees", uniqueConstraints = {
        @UniqueConstraint(name = "uk_season_employee", columnNames = { "season_id", "employee_user_id" })
}, indexes = {
        @Index(name = "idx_season_employee_season", columnList = "season_id"),
        @Index(name = "idx_season_employee_user", columnList = "employee_user_id")
})
public class SeasonEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "season_id", nullable = false)
    Season season;

    @ManyToOne
    @JoinColumn(name = "employee_user_id", nullable = false)
    org.example.QuanLyMuaVu.module.identity.entity.User employee;

    @ManyToOne
    @JoinColumn(name = "added_by_user_id")
    org.example.QuanLyMuaVu.module.identity.entity.User addedBy;

    @Column(name = "wage_per_task", precision = 15, scale = 2)
    BigDecimal wagePerTask;

    @Builder.Default
    @Column(name = "active", nullable = false)
    Boolean active = true;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }
}

