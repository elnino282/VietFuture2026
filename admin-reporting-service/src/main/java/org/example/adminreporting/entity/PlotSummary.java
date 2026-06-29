package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "admin_plot_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlotSummary {
    @Id
    @Column(name = "plot_id")
    private Integer plotId;

    @Column(name = "plot_name", nullable = false)
    private String plotName;

    @Column(name = "farm_id", nullable = false)
    private Integer farmId;

    @Column(name = "area", nullable = false, precision = 15, scale = 4)
    private BigDecimal area;
}
