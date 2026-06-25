package org.example.farm.dto.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmUpdatedEvent {
    private Integer farmId;
    private String farmName;
    private Long userId;
    private String provinceName;
    private String wardName;
    private String eventType; 
    private LocalDateTime timestamp;
}
