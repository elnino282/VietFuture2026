package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "admin_marketplace_product_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceProductSummary {
    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "farm_id")
    private Integer farmId;

    @Column(name = "farm_name")
    private String farmName;

    @Column(name = "farmer_id")
    private Long farmerId;

    @Column(name = "farmer_name")
    private String farmerName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
