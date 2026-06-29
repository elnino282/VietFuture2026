package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "admin_harvest_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HarvestSummary {
    @Id
    @Column(name = "harvest_id")
    private Integer harvestId;

    @Column(name = "season_id", nullable = false)
    private Integer seasonId;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitPrice;
}
