package org.example.QuanLyMuaVu.module.season.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "harvests")
public class Harvest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "harvest_id")
    Integer id;

    @ManyToOne
    @JoinColumn(name = "season_id")
    Season season;

    @Column(name = "harvest_date", nullable = false)
    LocalDate harvestDate;

    @Column(name = "quantity", nullable = false)
    BigDecimal quantity;

    @Column(name = "unit", nullable = false)
    BigDecimal unit;

    @Column(name = "grade", length = 20)
    String grade;

    String note;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
