package org.example.season.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.dto.response.DiseaseRecordDetailResponse;
import org.example.season.dto.response.DiseaseRecordResponse;
import org.example.season.dto.response.DiseaseTreatmentResponse;
import org.example.season.entity.DiseaseRecord;
import org.example.season.entity.DiseaseTreatment;
import org.example.season.entity.Season;
import org.example.season.service.ExternalServiceClient;
import org.example.season.service.SeasonWorkspaceAccessService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DiseaseRecordMapper {

    SeasonWorkspaceAccessService seasonWorkspaceAccessService;
    ExternalServiceClient externalServiceClient;

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
                : season != null ? season.getPlotId() : null;
        Integer resolvedCropId = diseaseRecord.getCropId() != null
                ? diseaseRecord.getCropId()
                : season != null ? season.getCropId() : null;
        Integer resolvedVarietyId = diseaseRecord.getVarietyId() != null
                ? diseaseRecord.getVarietyId()
                : season != null ? season.getVarietyId() : null;

        String plotName = null;
        if (resolvedPlotId != null) {
            ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(resolvedPlotId);
            if (plot != null) {
                plotName = plot.getPlotName();
            }
        }

        String cropName = null;
        if (resolvedCropId != null) {
            ExternalServiceClient.CropInternalDto crop = externalServiceClient.getCrop(resolvedCropId);
            if (crop != null) {
                cropName = crop.getCropName();
            }
        }

        String varietyName = null;
        if (resolvedVarietyId != null) {
            ExternalServiceClient.VarietyInternalDto variety = externalServiceClient.getVariety(resolvedVarietyId);
            if (variety != null) {
                varietyName = variety.getName();
            }
        }

        String reportedByUsername = null;
        String reportedByDisplayName = null;
        String reportedByType = null;
        if (diseaseRecord.getReportedByUserId() != null) {
            ExternalServiceClient.UserInternalDto user = externalServiceClient.getUser(diseaseRecord.getReportedByUserId());
            if (user != null) {
                reportedByUsername = user.getUsername();
                reportedByDisplayName = seasonWorkspaceAccessService.resolveDisplayName(user);
                reportedByType = seasonWorkspaceAccessService.resolveActorType(season, user);
            }
        }

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
                .reportedByUsername(reportedByUsername)
                .reportedByDisplayName(reportedByDisplayName)
                .reportedByType(reportedByType)
                .canEdit(seasonWorkspaceAccessService.canCurrentUserManageRecord(season, diseaseRecord.getReportedByUserId()))
                .canDelete(seasonWorkspaceAccessService.canCurrentUserManageRecord(season, diseaseRecord.getReportedByUserId()))
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
        Season season = treatment.getDiseaseRecord() != null ? treatment.getDiseaseRecord().getSeason() : null;

        String supplyItemName = null;
        if (treatment.getSupplyItemId() != null) {
            supplyItemName = externalServiceClient.getSupplyItemName(treatment.getSupplyItemId());
        }

        String batchCode = null;
        if (treatment.getSupplyLotId() != null) {
            batchCode = externalServiceClient.getSupplyLotBatchCode(treatment.getSupplyLotId());
        }

        String createdByUsername = null;
        String createdByDisplayName = null;
        String createdByType = null;
        if (treatment.getCreatedByUserId() != null) {
            ExternalServiceClient.UserInternalDto user = externalServiceClient.getUser(treatment.getCreatedByUserId());
            if (user != null) {
                createdByUsername = user.getUsername();
                createdByDisplayName = seasonWorkspaceAccessService.resolveDisplayName(user);
                createdByType = seasonWorkspaceAccessService.resolveActorType(season, user);
            }
        }

        return DiseaseTreatmentResponse.builder()
                .id(treatment.getId())
                .diseaseRecordId(treatment.getDiseaseRecord() != null ? treatment.getDiseaseRecord().getId() : null)
                .treatedAt(treatment.getTreatedAt())
                .method(treatment.getMethod())
                .supplyItemId(treatment.getSupplyItemId())
                .supplyItemName(supplyItemName)
                .supplyLotId(treatment.getSupplyLotId())
                .batchCode(batchCode)
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
                .createdByUsername(createdByUsername)
                .createdByDisplayName(createdByDisplayName)
                .createdByType(createdByType)
                .canEdit(seasonWorkspaceAccessService.canCurrentUserManageRecord(season, treatment.getCreatedByUserId()))
                .canDelete(seasonWorkspaceAccessService.canCurrentUserManageRecord(season, treatment.getCreatedByUserId()))
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
