package org.example.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "delivery_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "provider_id", nullable = false)
    private Integer providerId;

    @Column(name = "zone_from", nullable = false, length = 50)
    private String zoneFrom;

    @Column(name = "zone_to", nullable = false, length = 50)
    private String zoneTo;

    @Column(name = "weight_min_kg", nullable = false)
    @Builder.Default
    private BigDecimal weightMinKg = BigDecimal.ZERO;

    @Column(name = "weight_max_kg", nullable = false)
    private BigDecimal weightMaxKg;

    @Column(name = "base_rate_vnd", nullable = false)
    private BigDecimal baseRateVnd;

    @Column(name = "per_kg_vnd", nullable = false)
    @Builder.Default
    private BigDecimal perKgVnd = BigDecimal.ZERO;

    @Column(name = "estimated_hours")
    @Builder.Default
    private Integer estimatedHours = 48;

    @Column(name = "is_cold_chain")
    @Builder.Default
    private Boolean isColdChain = false;

    @Column(name = "cold_chain_fee_vnd")
    @Builder.Default
    private BigDecimal coldChainFeeVnd = BigDecimal.ZERO;
}
