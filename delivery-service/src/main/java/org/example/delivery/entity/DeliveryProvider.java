package org.example.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "supports_cold_chain")
    @Builder.Default
    private Boolean supportsColdChain = false;

    @Column(name = "supports_same_day")
    @Builder.Default
    private Boolean supportsSameDay = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
