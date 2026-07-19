package org.example.season.service;

import lombok.RequiredArgsConstructor;
import org.example.season.client.SustainabilityServiceClient;
import org.example.season.dto.response.ProductionDiaryEventDto;
import org.example.season.entity.FieldLog;
import org.example.season.entity.Harvest;
import org.example.season.entity.PesticideRecord;
import org.example.season.repository.FieldLogRepository;
import org.example.season.repository.HarvestRepository;
import org.example.season.repository.PesticideRecordRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductionDiaryAggregationService {

    private final FieldLogRepository fieldLogRepository;
    private final PesticideRecordRepository pesticideRecordRepository;
    private final HarvestRepository harvestRepository;
    private final SustainabilityServiceClient sustainabilityServiceClient;

    public List<ProductionDiaryEventDto> getProductionDiary(Integer seasonId) {
        List<ProductionDiaryEventDto> events = new ArrayList<>();

        // 1. Lấy FieldLogs
        List<FieldLog> fieldLogs = fieldLogRepository.findAllBySeasonId(seasonId);
        for (FieldLog log : fieldLogs) {
            events.add(ProductionDiaryEventDto.builder()
                    .eventDate(log.getLogDate())
                    .eventType(\"FIELD_LOG\")
                    .title(\"Nhật ký đồng ruộng\")
                    .description(log.getDescription())
                    .sourceService(\"season-service\")
                    .sourceId(log.getId())
                    .build());
        }

        // 2. Lấy PesticideRecords
        List<PesticideRecord> pesticideRecords = pesticideRecordRepository.findBySeasonId(seasonId);
        for (PesticideRecord rec : pesticideRecords) {
            events.add(ProductionDiaryEventDto.builder()
                    .eventDate(rec.getApplicationDate())
                    .eventType(\"PESTICIDE\")
                    .title(\"Phun thuốc BVTV: \" + rec.getPesticideName())
                    .description(\"Mục đích: \" + rec.getPurpose() + \". Liều lượng: \" + rec.getDosage())
                    .sourceService(\"season-service\")
                    .sourceId(rec.getId())
                    .build());
        }

        // 3. Lấy Harvests
        List<Harvest> harvests = harvestRepository.findAllBySeasonId(seasonId);
        for (Harvest harvest : harvests) {
            events.add(ProductionDiaryEventDto.builder()
                    .eventDate(harvest.getHarvestDate())
                    .eventType(\"HARVEST\")
                    .title(\"Thu hoạch\")
                    .description(\"Số lượng: \" + harvest.getQuantity() + \" (Lô: \" + harvest.getLotCode() + \")\")
                    .sourceService(\"season-service\")
                    .sourceId(harvest.getId())
                    .build());
        }

        // 4. Lấy từ sustainability-service
        try {
            List<SustainabilityServiceClient.NutrientInputEventInternalDto> nutrients = sustainabilityServiceClient.getNutrientInputs(seasonId);
            for (var nutrient : nutrients) {
                events.add(ProductionDiaryEventDto.builder()
                        .eventDate(nutrient.getAppliedDate())
                        .eventType(\"FERTILIZER\")
                        .title(\"Bón phân: \" + nutrient.getInputSource())
                        .description(\"Lượng N(kg): \" + nutrient.getNKg())
                        .sourceService(\"sustainability-service\")
                        .sourceId(nutrient.getId())
                        .build());
            }
        } catch (Exception e) {
            // Log error
        }

        try {
            List<SustainabilityServiceClient.SoilTestInternalDto> soilTests = sustainabilityServiceClient.getSoilTests(seasonId);
            for (var soil : soilTests) {
                events.add(ProductionDiaryEventDto.builder()
                        .eventDate(soil.getSampleDate())
                        .eventType(\"SOIL_TEST\")
                        .title(\"Kiểm tra đất\")
                        .description(\"Đã đo lường: \" + (soil.getMeasured() ? \"Có\" : \"Không\"))
                        .sourceService(\"sustainability-service\")
                        .sourceId(soil.getId())
                        .build());
            }
        } catch (Exception e) {
            // Log error
        }

        try {
            List<SustainabilityServiceClient.IrrigationWaterAnalysisInternalDto> waterAnalyses = sustainabilityServiceClient.getWaterAnalyses(seasonId);
            for (var water : waterAnalyses) {
                events.add(ProductionDiaryEventDto.builder()
                        .eventDate(water.getSampleDate())
                        .eventType(\"IRRIGATION\")
                        .title(\"Phân tích nước tưới\")
                        .description(\"Đã đo lường: \" + (water.getMeasured() ? \"Có\" : \"Không\"))
                        .sourceService(\"sustainability-service\")
                        .sourceId(water.getId())
                        .build());
            }
        } catch (Exception e) {
            // Log error
        }

        // Sắp xếp theo ngày (mới nhất lên đầu hoặc cũ nhất lên đầu tuỳ vào requirements. Sẽ sort mới nhất lên đầu)
        events.sort(Comparator.comparing(ProductionDiaryEventDto::getEventDate, Comparator.nullsLast(Comparator.reverseOrder())));

        return events;
    }
}

