package org.example.adminreporting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {
    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "processed_at", insertable = false, updatable = false)
    private LocalDateTime processedAt;
}
