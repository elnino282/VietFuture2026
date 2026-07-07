package org.example.farm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "certification_standards")
public class CertificationStandard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(nullable = false, unique = true, length = 50)
    String code;

    @Column(nullable = false)
    String name;

    @Column(nullable = false, length = 30)
    String type;

    @Column(length = 20)
    @Builder.Default
    String version = "1.0";

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "is_active")
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
