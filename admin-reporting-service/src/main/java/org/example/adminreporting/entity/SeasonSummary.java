package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "admin_season_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonSummary {
    @Id
    @Column(name = "season_id")
    private Integer seasonId;

    @Column(name = "season_name", nullable = false)
    private String seasonName;

    @Column(name = "plot_id", nullable = false)
    private Integer plotId;

    @Column(name = "crop_id")
    private Integer cropId;

    @Column(name = "crop_name")
    private String cropName;

    @Column(name = "variety_id")
    private Integer varietyId;

    @Column(name = "variety_name")
    private String varietyName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_yield_kg", precision = 15, scale = 4)
    private BigDecimal expectedYieldKg;

    @Column(name = "actual_yield_kg", precision = 15, scale = 4)
    private BigDecimal actualYieldKg;
}
