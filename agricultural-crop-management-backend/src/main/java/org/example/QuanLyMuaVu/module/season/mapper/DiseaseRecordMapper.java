package org.example.QuanLyMuaVu.module.season.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseRecordDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseRecordResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseTreatmentResponse;
import org.example.QuanLyMuaVu.module.season.entity.DiseaseRecord;
import org.example.QuanLyMuaVu.module.season.entity.DiseaseTreatment;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.springframework.stereotype.Component;

@Component
public class DiseaseRecordMapper {

    public DiseaseRecordResponse toDiseaseRecordResponse(
            DiseaseRecord diseaseRecord,
            Long treatmentCount,
            LocalDateTime latestTreatmentAt) {
        if (diseaseRecord == null) {
            return null;
        }

        Season season = diseaseRecord.getSeason();
        Integer resolvedPlotId = diseaseRecord.getPlotId() != null
                ? diseaseRecord.getPlotId()
                : season != null && season.getPlot() != null ? season.getPlot().getId() : null;
        Integer resolvedCropId = diseaseRecord.getCropId() != null
                ? diseaseRecord.getCropId()
                : season != null && season.getCrop() != null ? season.getCrop().getId() : null;
        Integer resolvedVarietyId = diseaseRecord.getVarietyId() != null
                ? diseaseRecord.getVarietyId()
                : season != null && season.getVariety() != null ? season.getVariety().getId() : null;

        String plotName = season != null && season.getPlot() != null
                ? season.getPlot().getPlotName()
                : diseaseRecord.getPlot() != null ? diseaseRecord.getPlot().getPlotName() : null;
        String cropName = season != null && season.getCrop() != null
                ? season.getCrop().getCropName()
                : diseaseRecord.getCrop() != null ? diseaseRecord.getCrop().getCropName() : null;
        String varietyName = season != null && season.getVariety() != null
                ? season.getVariety().getName()
                : diseaseRecord.getVariety() != null ? diseaseRecord.getVariety().getName() : null;

        return DiseaseRecordResponse.builder()
                .id(diseaseRecord.getId())
                .seasonId(season != null ? season.getId() : null)
                .seasonName(season != null ? season.getSeasonName() : null)
                .plotId(resolvedPlotId)
                .plotName(plotName)
                .cropId(resolvedCropId)
                .cropName(cropName)
                .varietyId(resolvedVarietyId)
                .varietyName(varietyName)
                .reportedByUserId(diseaseRecord.getReportedByUserId())
                .reportedByUsername(diseaseRecord.getReportedBy() != null ? diseaseRecord.getReportedBy().getUsername() : null)
                .incidentId(diseaseRecord.getIncidentId())
                .diseaseName(diseaseRecord.getDiseaseName())
                .symptomSummary(diseaseRecord.getSymptomSummary())
                .severity(diseaseRecord.getSeverity() != null ? diseaseRecord.getSeverity().name() : null)
                .status(diseaseRecord.getStatus() != null ? diseaseRecord.getStatus().name() : null)
                .detectedAt(diseaseRecord.getDetectedAt())
                .affectedPlantCount(diseaseRecord.getAffectedPlantCount())
                .affectedAreaValue(diseaseRecord.getAffectedAreaValue())
                .affectedAreaUnit(diseaseRecord.getAffectedAreaUnit())
                .evidenceUrl(diseaseRecord.getEvidenceUrl())
                .notes(diseaseRecord.getNotes())
                .createdAt(diseaseRecord.getCreatedAt())
                .updatedAt(diseaseRecord.getUpdatedAt())
                .treatmentCount(treatmentCount)
                .latestTreatmentAt(latestTreatmentAt)
                .build();
    }

    public DiseaseTreatmentResponse toDiseaseTreatmentResponse(DiseaseTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        return DiseaseTreatmentResponse.builder()
                .id(treatment.getId())
                .diseaseRecordId(treatment.getDiseaseRecord() != null ? treatment.getDiseaseRecord().getId() : null)
                .treatedAt(treatment.getTreatedAt())
                .method(treatment.getMethod())
                .supplyItemId(treatment.getSupplyItemId())
                .supplyItemName(treatment.getSupplyItem() != null ? treatment.getSupplyItem().getName() : null)
                .supplyLotId(treatment.getSupplyLotId())
                .batchCode(treatment.getSupplyLot() != null ? treatment.getSupplyLot().getBatchCode() : null)
                .materialName(treatment.getMaterialName())
                .quantityUsed(treatment.getQuantityUsed())
                .unit(treatment.getUnit())
                .costAmount(treatment.getCostAmount())
                .expenseId(treatment.getExpenseId())
                .effectiveness(treatment.getEffectiveness() != null ? treatment.getEffectiveness().name() : null)
                .resultSummary(treatment.getResultSummary())
                .nextReviewAt(treatment.getNextReviewAt())
                .notes(treatment.getNotes())
                .createdByUserId(treatment.getCreatedByUserId())
                .createdByUsername(treatment.getCreatedBy() != null ? treatment.getCreatedBy().getUsername() : null)
                .createdAt(treatment.getCreatedAt())
                .updatedAt(treatment.getUpdatedAt())
                .build();
    }

    public DiseaseRecordDetailResponse toDiseaseRecordDetailResponse(
            DiseaseRecord diseaseRecord,
            List<DiseaseTreatmentResponse> treatments,
            Long treatmentCount,
            LocalDateTime latestTreatmentAt,
            BigDecimal totalTreatmentCost) {
        return DiseaseRecordDetailResponse.builder()
                .record(toDiseaseRecordResponse(diseaseRecord, treatmentCount, latestTreatmentAt))
                .treatments(treatments)
                .treatmentCount(treatmentCount)
                .latestTreatmentAt(latestTreatmentAt)
                .totalTreatmentCost(totalTreatmentCost)
                .build();
    }
}
