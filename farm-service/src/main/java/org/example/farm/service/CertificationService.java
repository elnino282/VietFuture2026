package org.example.farm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.farm.dto.request.UpdateCertificationItemRequest;
import org.example.farm.dto.response.CertificationDetailsResponse;
import org.example.farm.entity.CertificationChecklistItem;
import org.example.farm.entity.CertificationItemStatus;
import org.example.farm.entity.CertificationRecord;
import org.example.farm.entity.CertificationStandard;
import org.example.farm.exception.AppException;
import org.example.farm.exception.ErrorCode;
import org.example.farm.repository.CertificationChecklistItemRepository;
import org.example.farm.repository.CertificationItemStatusRepository;
import org.example.farm.repository.CertificationRecordRepository;
import org.example.farm.repository.CertificationStandardRepository;
import org.example.farm.repository.FarmRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CertificationService {

    private final CertificationStandardRepository standardRepository;
    private final CertificationChecklistItemRepository checklistItemRepository;
    private final CertificationRecordRepository recordRepository;
    private final CertificationItemStatusRepository itemStatusRepository;
    private final CertificationScoringService scoringService;
    private final FarmRepository farmRepository;

    private static final BigDecimal ELIGIBILITY_THRESHOLD = new BigDecimal("80.00");

    /**
     * Lấy hoặc khởi tạo certification record cho farm + standard.
     * Nếu chưa có record → tự động tạo record + item statuses.
     */
    public CertificationDetailsResponse getOrInitCertification(Integer farmId, String standardCode, Long userId) {
        farmRepository.findById(farmId)
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        CertificationStandard standard = standardRepository.findByCode(standardCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        CertificationRecord record = recordRepository.findByFarmIdAndStandardId(farmId, standard.getId())
                .orElseGet(() -> initCertificationRecord(farmId, standard));

        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(standard.getId());
        List<CertificationItemStatus> statuses = itemStatusRepository.findByRecordId(record.getId());

        return buildResponse(record, standard, items, statuses);
    }

    /**
     * Auto-fill checklist items từ dữ liệu hệ thống (soil tests, water tests, field logs, PHI).
     */
    public CertificationDetailsResponse autoPopulate(Integer farmId, String standardCode, Long userId) {
        farmRepository.findById(farmId)
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        CertificationStandard standard = standardRepository.findByCode(standardCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        CertificationRecord record = recordRepository.findByFarmIdAndStandardId(farmId, standard.getId())
                .orElseGet(() -> initCertificationRecord(farmId, standard));

        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(standard.getId());
        List<CertificationItemStatus> statuses = itemStatusRepository.findByRecordId(record.getId());

        // Auto-populate từ field logs, soil tests, water tests, PHI check
        scoringService.autoPopulateFromFieldLogs(farmId, statuses, items);

        // Save updated statuses
        itemStatusRepository.saveAll(statuses);

        // Recalculate score
        BigDecimal score = scoringService.calculateScore(statuses, items);
        record.setComplianceScore(score);

        // Update status nếu đủ điều kiện
        if (score.compareTo(ELIGIBILITY_THRESHOLD) >= 0 && "IN_PROGRESS".equals(record.getStatus())) {
            record.setStatus("READY_TO_APPLY");
        }

        recordRepository.save(record);

        return buildResponse(record, standard, items, statuses);
    }

    /**
     * Cập nhật manual override cho một checklist item.
     */
    public CertificationDetailsResponse updateItemStatus(
            Integer farmId, String standardCode, Integer itemId,
            UpdateCertificationItemRequest request, Long userId) {

        farmRepository.findById(farmId)
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        CertificationStandard standard = standardRepository.findByCode(standardCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        CertificationRecord record = recordRepository.findByFarmIdAndStandardId(farmId, standard.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        CertificationItemStatus itemStatus = itemStatusRepository
                .findByRecordIdAndChecklistItemId(record.getId(), itemId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Update fields
        if (request.getStatus() != null) {
            itemStatus.setStatus(request.getStatus());
        }
        if (request.getEvidenceUrl() != null) {
            itemStatus.setEvidenceUrl(request.getEvidenceUrl());
        }
        if (request.getNotes() != null) {
            itemStatus.setNotes(request.getNotes());
        }
        itemStatus.setCheckedAt(LocalDateTime.now());
        itemStatus.setCheckedBy(userId);

        itemStatusRepository.save(itemStatus);

        // Recalculate score
        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(standard.getId());
        List<CertificationItemStatus> statuses = itemStatusRepository.findByRecordId(record.getId());
        BigDecimal score = scoringService.calculateScore(statuses, items);
        record.setComplianceScore(score);

        if (score.compareTo(ELIGIBILITY_THRESHOLD) >= 0 && "IN_PROGRESS".equals(record.getStatus())) {
            record.setStatus("READY_TO_APPLY");
        } else if (score.compareTo(ELIGIBILITY_THRESHOLD) < 0 && "READY_TO_APPLY".equals(record.getStatus())) {
            record.setStatus("IN_PROGRESS");
        }

        recordRepository.save(record);

        return buildResponse(record, standard, items, statuses);
    }

    /**
     * Nộp đơn xin chứng nhận — chỉ cho phép khi score ≥ 80%.
     */
    public CertificationDetailsResponse applyCertification(Integer farmId, String standardCode, Long userId) {
        farmRepository.findById(farmId)
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        CertificationStandard standard = standardRepository.findByCode(standardCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        CertificationRecord record = recordRepository.findByFarmIdAndStandardId(farmId, standard.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(standard.getId());
        List<CertificationItemStatus> statuses = itemStatusRepository.findByRecordId(record.getId());
        BigDecimal score = scoringService.calculateScore(statuses, items);

        if (score.compareTo(ELIGIBILITY_THRESHOLD) < 0) {
            throw new IllegalArgumentException(
                    "Compliance score (" + score + "%) chưa đạt ngưỡng 80%. " +
                    "Vui lòng hoàn thành thêm các tiêu chí VietGAP.");
        }

        record.setStatus("APPLIED");
        record.setAppliedAt(LocalDateTime.now());
        record.setComplianceScore(score);
        recordRepository.save(record);

        return buildResponse(record, standard, items, statuses);
    }

    // ─── Helpers ───

    private CertificationRecord initCertificationRecord(Integer farmId, CertificationStandard standard) {
        CertificationRecord record = CertificationRecord.builder()
                .farmId(farmId)
                .standardId(standard.getId())
                .status("IN_PROGRESS")
                .complianceScore(BigDecimal.ZERO)
                .build();
        record = recordRepository.save(record);

        // Tạo item statuses cho tất cả checklist items
        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(standard.getId());
        List<CertificationItemStatus> statuses = new ArrayList<>();
        for (CertificationChecklistItem item : items) {
            statuses.add(CertificationItemStatus.builder()
                    .recordId(record.getId())
                    .checklistItemId(item.getId())
                    .status("PENDING")
                    .build());
        }
        itemStatusRepository.saveAll(statuses);
        return record;
    }

    private CertificationDetailsResponse buildResponse(
            CertificationRecord record,
            CertificationStandard standard,
            List<CertificationChecklistItem> items,
            List<CertificationItemStatus> statuses) {

        List<CertificationDetailsResponse.CertificationItemDetail> itemDetails = items.stream()
                .map(item -> {
                    CertificationItemStatus status = statuses.stream()
                            .filter(s -> s.getChecklistItemId().equals(item.getId()))
                            .findFirst().orElse(null);

                    return CertificationDetailsResponse.CertificationItemDetail.builder()
                            .id(item.getId())
                            .itemCode(item.getItemCode())
                            .category(item.getCategory())
                            .description(item.getDescription())
                            .isMandatory(item.getIsMandatory())
                            .weightPct(item.getWeightPct())
                            .dataSourceType(item.getDataSourceType())
                            .dataSourceQuery(item.getDataSourceQuery())
                            .status(status != null ? status.getStatus() : "PENDING")
                            .evidenceUrl(status != null ? status.getEvidenceUrl() : null)
                            .notes(status != null ? status.getNotes() : null)
                            .checkedAt(status != null ? status.getCheckedAt() : null)
                            .build();
                })
                .toList();

        BigDecimal score = record.getComplianceScore() != null ? record.getComplianceScore() : BigDecimal.ZERO;

        return CertificationDetailsResponse.builder()
                .recordId(record.getId())
                .farmId(record.getFarmId())
                .standardCode(standard.getCode())
                .standardName(standard.getName())
                .complianceScore(score)
                .status(record.getStatus())
                .appliedAt(record.getAppliedAt())
                .certifiedAt(record.getCertifiedAt())
                .expiryDate(record.getExpiryDate())
                .auditorNotes(record.getAuditorNotes())
                .items(itemDetails)
                .isEligible(score.compareTo(ELIGIBILITY_THRESHOLD) >= 0)
                .build();
    }
}
