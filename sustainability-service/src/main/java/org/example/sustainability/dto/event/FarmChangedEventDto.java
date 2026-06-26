package org.example.sustainability.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmChangedEventDto {
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String producer;
    private String action;
    private Integer farmId;
    private Long userId;
    private String farmName;
    private Integer provinceId;
    private String provinceName;
    private Integer wardId;
    private String wardName;
    private java.math.BigDecimal area;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private Boolean active;
}
