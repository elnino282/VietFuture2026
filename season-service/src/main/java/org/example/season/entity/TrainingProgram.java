package org.example.season.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = \"training_programs\")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = \"TEXT\")
    private String description;

    @Column(name = \"is_mandatory\")
    private Boolean isMandatory;

    @Column(name = \"created_at\", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isMandatory == null) {
            isMandatory = false;
        }
    }
}

