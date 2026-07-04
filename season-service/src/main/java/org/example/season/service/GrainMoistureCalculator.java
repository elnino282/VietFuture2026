package org.example.season.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GrainMoistureCalculator {

    /**
     * Tính toán khối lượng khô thực tế của ngũ cốc sau khi sấy (hoặc phơi) 
     * dựa trên độ ẩm ban đầu, độ ẩm mục tiêu và tỉ lệ thất thoát cơ học.
     * 
     * Công thức: Khối lượng khô = Khối lượng ướt * ((100 - % Độ ẩm thực tế ban đầu) / (100 - % Độ ẩm mục tiêu)) 
     *                           - (Khối lượng ướt * % Thất thoát cơ học)
     *
     * @param grossWetWeight            Khối lượng ướt gặt ngoài đồng
     * @param currentMoisturePercentage % Độ ẩm thực tế ban đầu (ví dụ: 25.0 cho 25%)
     * @param targetMoisturePercentage  % Độ ẩm mục tiêu (ví dụ: 14.0 cho 14%)
     * @param mechanicalLossPercentage  % Thất thoát cơ học (ví dụ: 1.5 cho 1.5%)
     * @return Khối lượng khô tịnh (net dry weight)
     */
    public static BigDecimal calculateNetDryWeight(BigDecimal grossWetWeight, 
                                                   BigDecimal currentMoisturePercentage, 
                                                   BigDecimal targetMoisturePercentage, 
                                                   BigDecimal mechanicalLossPercentage) {
        if (grossWetWeight == null) return BigDecimal.ZERO;
        
        BigDecimal oneHundred = new BigDecimal("100");
        
        BigDecimal currentMoisture = currentMoisturePercentage != null ? currentMoisturePercentage : BigDecimal.ZERO;
        BigDecimal targetMoisture = targetMoisturePercentage != null ? targetMoisturePercentage : BigDecimal.ZERO;
        BigDecimal mechLoss = mechanicalLossPercentage != null ? mechanicalLossPercentage : BigDecimal.ZERO;

        // Tránh chia cho 0 nếu độ ẩm mục tiêu = 100% (phi logic thực tế nhưng cần safe check)
        if (oneHundred.compareTo(targetMoisture) == 0) {
            throw new IllegalArgumentException("Target moisture cannot be 100%");
        }

        // Tính tỉ lệ hao hụt do bốc hơi nước: (100 - currentMoisture) / (100 - targetMoisture)
        BigDecimal moistureFactor = oneHundred.subtract(currentMoisture)
                .divide(oneHundred.subtract(targetMoisture), 4, RoundingMode.HALF_UP);

        // Khối lượng sau khi bốc hơi nước
        BigDecimal weightAfterDrying = grossWetWeight.multiply(moistureFactor);

        // Tính khối lượng bị thất thoát cơ học (rơi vãi, lẫn tạp chất bị loại bỏ...)
        BigDecimal mechanicalLossWeight = grossWetWeight.multiply(mechLoss).divide(oneHundred, 4, RoundingMode.HALF_UP);

        // Khối lượng khô tịnh cuối cùng
        BigDecimal netDryWeight = weightAfterDrying.subtract(mechanicalLossWeight);

        // Đảm bảo không âm
        if (netDryWeight.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return netDryWeight.setScale(2, RoundingMode.HALF_UP);
    }
}
