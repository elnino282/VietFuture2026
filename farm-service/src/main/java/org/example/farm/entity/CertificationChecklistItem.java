package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "certification_checklist_items")
public class CertificationChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "standard_id", nullable = false)
    Integer standardId;

    @Column(name = "item_code", nullable = false, length = 50)
    String itemCode;

    @Column(nullable = false, length = 50)
    String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    String description;

    @Column(name = "is_mandatory")
    @Builder.Default
    Boolean isMandatory = true;

    @Column(name = "weight_pct", precision = 5, scale = 2)
    @Builder.Default
    BigDecimal weightPct = BigDecimal.ONE;

    @Column(name = "data_source_type", length = 30)
    String dataSourceType;

    @Column(name = "data_source_query", length = 500)
    String dataSourceQuery;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
