package org.example.season.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
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
@Table(name = "pesticide_phi_reference")
public class PesticidePHIReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "active_ingredient", nullable = false)
    String activeIngredient;

    @Column(name = "pesticide_name")
    String pesticideName;

    @Column(name = "phi_days", nullable = false)
    Integer phiDays;

    @Column(name = "mrl_mg_per_kg")
    BigDecimal mrlMgPerKg;

    @Column(name = "crop_type")
    String cropType;

    @Builder.Default
    String source = "EPA/CODEX";

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
