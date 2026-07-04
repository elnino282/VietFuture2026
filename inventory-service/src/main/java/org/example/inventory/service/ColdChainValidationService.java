package org.example.inventory.service;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.inventory.entity.ProductWarehouseLot;
import org.example.inventory.entity.Warehouse;
import org.example.inventory.enums.StorageCategory;
import org.example.inventory.exception.AppException;
import org.example.inventory.exception.ColdChainViolationException;
import org.example.inventory.exception.ErrorCode;
import org.example.inventory.repository.ProductWarehouseLotRepository;
import org.example.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColdChainValidationService {

    private final ProductWarehouseLotRepository productWarehouseLotRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Rule 1: Validate sinh lý nông sản với loại kho vật lý
     */
    public void validateStorageCategory(String cropCategory, StorageCategory storageCategory) {
        if (cropCategory == null || storageCategory == null) {
            return; // Not enough info to validate
        }

        boolean isColdSensitive = cropCategory.equalsIgnoreCase("VEGETABLE") 
                               || cropCategory.equalsIgnoreCase("FRUIT");

        if (isColdSensitive && storageCategory == StorageCategory.DRY) {
            throw new ColdChainViolationException(
                String.format("Cảnh báo chuỗi cung ứng: Nông sản thuộc nhóm [%s] có rủi ro hư hỏng cao, không được phép lưu trữ trong kho %s.", 
                              cropCategory, storageCategory.name())
            );
        }
    }

    /**
     * Rule 2: Kiểm tra Telemetry thời gian thực (Giả lập IoT Alert)
     */
    public void checkTelemetry(Integer lotId, BigDecimal currentTemp, BigDecimal currentHumidity) {
        ProductWarehouseLot lot = productWarehouseLotRepository.findById(lotId)
                .orElseThrow(() -> new AppException(ErrorCode.LOT_NOT_FOUND));

        Warehouse warehouse = warehouseRepository.findById(lot.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        boolean tempViolated = false;
        boolean humidityViolated = false;

        if (currentTemp != null) {
            if (warehouse.getTemperatureMin() != null && currentTemp.compareTo(warehouse.getTemperatureMin()) < 0) {
                tempViolated = true;
            }
            if (warehouse.getTemperatureMax() != null && currentTemp.compareTo(warehouse.getTemperatureMax()) > 0) {
                tempViolated = true;
            }
        }

        if (currentHumidity != null) {
            if (warehouse.getHumidityMin() != null && currentHumidity.compareTo(warehouse.getHumidityMin()) < 0) {
                humidityViolated = true;
            }
            if (warehouse.getHumidityMax() != null && currentHumidity.compareTo(warehouse.getHumidityMax()) > 0) {
                humidityViolated = true;
            }
        }

        if (tempViolated || humidityViolated) {
            // Giả lập push alert khẩn cấp
            log.warn("🚨 CẢNH BÁO TỔN THƯƠNG DO LẠNH/NHIỆT: Lô hàng {} (Mã: {}) tại Kho {} đang ở điều kiện nguy hiểm! Temp: {}, Hum: {}",
                    lot.getId(), lot.getLotCode(), warehouse.getName(), currentTemp, currentHumidity);
        }
    }
}
