package org.example.inventory.dto.request;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReceiveToWarehouseRequest {
    /**
     * Độ ẩm hiện tại (%) — bắt buộc cho ngũ cốc (GRAIN).
     */
    BigDecimal currentMoisture;

    /**
     * Độ ẩm mục tiêu (%) — bắt buộc cho ngũ cốc (GRAIN).
     */
    BigDecimal targetMoisture;

    /**
     * Thất thoát cơ học (kg) — tuỳ chọn.
     */
    BigDecimal mechanicalLoss;
}
