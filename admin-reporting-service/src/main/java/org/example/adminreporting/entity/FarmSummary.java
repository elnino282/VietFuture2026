package org.example.adminreporting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_farm_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmSummary {
    @Id
    @Column(name = "farm_id")
    private Integer farmId;

    @Column(name = "farm_name", nullable = false)
    private String farmName;
}
