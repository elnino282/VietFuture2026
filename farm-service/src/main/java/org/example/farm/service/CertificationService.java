package org.example.farm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.farm.dto.request.UpdateCertificationItemRequest;
import org.example.farm.dto.response.CertificationDetailsResponse;
import org.example.farm.dto.response.CertificationDetailsResponse.CertificationItemDetail;
import org.example.farm.entity.*;
import org.example.farm.exception.AppException;
import org.example.farm.exception.ErrorCode;
import org.example.farm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CertificationService {

    private final CertificationStandardRepository standardRepository;
    private final CertificationChecklistItemRepository checklistItemRepository;
    private final CertificationRecordRepository recordRepository;
    private final CertificationItemStatusRepository itemStatusRepository;
    private final CertificationScoringService scoringService;
    private final FarmRepository farmRepository;

    public CertificationRecord getOrCreateRecord(Integer farmId) {
        // Kiểm tra farm có tồn tại không
        farmRepository.findById(farmId)
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        CertificationStandard standard = standardRepository.findByCode("VIETGAP-PLANTING-2024")
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        Optional<CertificationRecord> recordOpt = recordRepository.findByFarmIdAndStandardId(farmId, standard.getId());
        if (recordOpt.isPresent()) {
            return recordOpt.get();
        }

        // Tạo bản ghi chứng nhận mới
        CertificationRecord record = CertificationRecord.builder()
                .farmId(farmId)
                .standardId(standard.getId())
                .complianceScore(BigDecimal.ZERO)
                .status("IN_PROGRESS")
                .build();
        record = recordRepository.save(record);

        // Tạo item statuses cho tất cả checklist items
        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(standard.getId());
        List<CertificationItemStatus> statuses = new ArrayList<>();
        for (CertificationChecklistItem item : items) {
            CertificationItemStatus status = CertificationItemStatus.builder()
                    .recordId(record.getId())
                    .checklistItemId(item.getId())
                    .status("PENDING")
                    .build();
            statuses.add(status);
        }
        itemStatusRepository.saveAll(statuses);

        return record;
    }

    public CertificationDetailsResponse getCertificationDetails(Integer farmId) {
        CertificationRecord record = getOrCreateRecord(farmId);
        List<CertificationItemStatus> statuses = itemStatusRepository.findByRecordId(record.getId());
        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(record.getStandardId());

        // Tự động điền (auto-populate) từ logs, tests, PHI check
        scoringService.autoPopulateFromFieldLogs(farmId, statuses, items);
        itemStatusRepository.saveAll(statuses);

        // Tính toán lại compliance score
        BigDecimal score = scoringService.calculateScore(statuses, items);
        record.setComplianceScore(score);

        // Cập nhật trạng thái tự động dựa trên score
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0 && "IN_PROGRESS".equals(record.getStatus())) {
            record.setStatus("READY_TO_APPLY");
        } else if (score.compareTo(BigDecimal.valueOf(80)) < 0 && "READY_TO_APPLY".equals(record.getStatus())) {
            record.setStatus("IN_PROGRESS");
        }
        recordRepository.save(record);

        CertificationStandard standard = standardRepository.findById(record.getStandardId()).orElse(null);

        List<CertificationItemDetail> itemDetails = new ArrayList<>();
        for (CertificationChecklistItem item : items) {
            CertificationItemStatus status = statuses.stream()
                    .filter(s -> s.getChecklistItemId().equals(item.getId()))
                    .findFirst().orElse(null);

            itemDetails.add(CertificationItemDetail.builder()
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
                    .build());
        }

        return CertificationDetailsResponse.builder()
                .recordId(record.getId())
                .farmId(record.getFarmId())
                .standardCode(standard != null ? standard.getCode() : "")
                .standardName(standard != null ? standard.getName() : "")
                .complianceScore(record.getComplianceScore())
                .status(record.getStatus())
                .appliedAt(record.getAppliedAt())
                .certifiedAt(record.getCertifiedAt())
                .expiryDate(record.getExpiryDate())
                .auditorNotes(record.getAuditorNotes())
                .items(itemDetails)
                .isEligible(record.getComplianceScore().compareTo(BigDecimal.valueOf(80)) >= 0)
                .build();
    }

    public void updateItemStatus(Integer farmId, Integer itemId, UpdateCertificationItemRequest req) {
        CertificationRecord record = getOrCreateRecord(farmId);
        CertificationItemStatus status = itemStatusRepository.findByRecordIdAndChecklistItemId(record.getId(), itemId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        if (req.getStatus() != null) {
            status.setStatus(req.getStatus().toUpperCase());
        }
        if (req.getEvidenceUrl() != null) {
            status.setEvidenceUrl(req.getEvidenceUrl());
        }
        if (req.getNotes() != null) {
            status.setNotes(req.getNotes());
        }
        status.setCheckedAt(LocalDateTime.now());
        itemStatusRepository.save(status);

        // Tính toán lại điểm số
        List<CertificationItemStatus> statuses = itemStatusRepository.findByRecordId(record.getId());
        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(record.getStandardId());
        BigDecimal score = scoringService.calculateScore(statuses, items);
        record.setComplianceScore(score);

        if (score.compareTo(BigDecimal.valueOf(80)) >= 0 && "IN_PROGRESS".equals(record.getStatus())) {
            record.setStatus("READY_TO_APPLY");
        } else if (score.compareTo(BigDecimal.valueOf(80)) < 0 && "READY_TO_APPLY".equals(record.getStatus())) {
            record.setStatus("IN_PROGRESS");
        }
        recordRepository.save(record);
    }

    public void apply(Integer farmId) {
        CertificationRecord record = getOrCreateRecord(farmId);

        // Lấy score hiện tại
        List<CertificationItemStatus> statuses = itemStatusRepository.findByRecordId(record.getId());
        List<CertificationChecklistItem> items = checklistItemRepository.findByStandardId(record.getStandardId());
        BigDecimal score = scoringService.calculateScore(statuses, items);

        if (score.compareTo(BigDecimal.valueOf(80)) < 0) {
            throw new IllegalArgumentException("Không đủ điều kiện: Điểm VietGAP phải đạt ít nhất 80%.");
        }

        record.setStatus("APPLIED");
        record.setAppliedAt(LocalDateTime.now());
        recordRepository.save(record);
    }
}
