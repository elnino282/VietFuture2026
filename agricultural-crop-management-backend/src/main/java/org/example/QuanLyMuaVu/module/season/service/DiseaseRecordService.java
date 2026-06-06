package org.example.QuanLyMuaVu.module.season.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.Enums.DiseaseSeverity;
import org.example.QuanLyMuaVu.Enums.DiseaseStatus;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.TreatmentEffectiveness;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.financial.entity.Expense;
import org.example.QuanLyMuaVu.module.financial.repository.ExpenseRepository;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.example.QuanLyMuaVu.module.incident.repository.IncidentRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.repository.InventoryBalanceRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.StockMovementRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyItemRepository;
import org.example.QuanLyMuaVu.module.inventory.repository.SupplyLotRepository;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateDiseaseRecordRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateDiseaseTreatmentRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateDiseaseRecordRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateDiseaseTreatmentRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseRecordDetailResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseRecordResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.DiseaseTreatmentResponse;
import org.example.QuanLyMuaVu.module.season.entity.DiseaseRecord;
import org.example.QuanLyMuaVu.module.season.entity.DiseaseTreatment;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.mapper.DiseaseRecordMapper;
import org.example.QuanLyMuaVu.module.season.repository.DiseaseRecordRepository;
import org.example.QuanLyMuaVu.module.season.repository.DiseaseTreatmentRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class DiseaseRecordService {

    DiseaseRecordRepository diseaseRecordRepository;
    DiseaseTreatmentRepository diseaseTreatmentRepository;
    SeasonRepository seasonRepository;
    IncidentRepository incidentRepository;
    ExpenseRepository expenseRepository;
    SupplyItemRepository supplyItemRepository;
    SupplyLotRepository supplyLotRepository;
    InventoryBalanceRepository inventoryBalanceRepository;
    StockMovementRepository stockMovementRepository;
    FarmAccessPort farmAccessService;
    AuditLogService auditLogService;
    DiseaseRecordMapper diseaseRecordMapper;
    SeasonWorkspaceAccessService seasonWorkspaceAccessService;

    public PageResponse<DiseaseRecordResponse> listDiseaseRecordsBySeason(
            Integer seasonId,
            String status,
            String severity,
            String q,
            LocalDate fromDetectedAt,
            LocalDate toDetectedAt,
            int page,
            int size) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        return listDiseaseRecordsForResolvedSeason(
                season,
                status,
                severity,
                q,
                fromDetectedAt,
                toDetectedAt,
                page,
                size);
    }

    @Transactional(readOnly = true)
    public PageResponse<DiseaseRecordResponse> listDiseaseRecordsByAssignedEmployeeSeason(
            Integer seasonId,
            String status,
            String severity,
            String q,
            LocalDate fromDetectedAt,
            LocalDate toDetectedAt,
            int page,
            int size) {
        Season season = getSeasonForCurrentEmployee(seasonId);
        return listDiseaseRecordsForResolvedSeason(
                season,
                status,
                severity,
                q,
                fromDetectedAt,
                toDetectedAt,
                page,
                size);
    }

    private PageResponse<DiseaseRecordResponse> listDiseaseRecordsForResolvedSeason(
            Season season,
            String status,
            String severity,
            String q,
            LocalDate fromDetectedAt,
            LocalDate toDetectedAt,
            int page,
            int size) {
        Specification<DiseaseRecord> spec = buildRecordSpecification(
                season.getId(),
                status,
                severity,
                q,
                fromDetectedAt,
                toDetectedAt);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("detectedAt"), Sort.Order.desc("id")));
        Page<DiseaseRecord> diseasePage = diseaseRecordRepository.findAll(spec, pageable);

        List<DiseaseRecordResponse> items = diseasePage.getContent()
                .stream()
                .map(record -> {
                    long treatmentCount = diseaseTreatmentRepository.countByDiseaseRecord_Id(record.getId());
                    LocalDateTime latestTreatmentAt = diseaseTreatmentRepository
                            .findLatestTreatedAtByDiseaseRecordId(record.getId());
                    return diseaseRecordMapper.toDiseaseRecordResponse(record, treatmentCount, latestTreatmentAt);
                })
                .toList();

        return PageResponse.of(diseasePage, items);
    }

    @Transactional(readOnly = true)
    public DiseaseRecordDetailResponse getDiseaseRecordDetail(Integer id) {
        DiseaseRecord diseaseRecord = getDiseaseRecordForCurrentFarmer(id);
        return getDiseaseRecordDetailForResolvedRecord(diseaseRecord);
    }

    @Transactional(readOnly = true)
    public DiseaseRecordDetailResponse getDiseaseRecordDetailForAssignedEmployee(Integer id) {
        DiseaseRecord diseaseRecord = getDiseaseRecordForCurrentEmployee(id);
        return getDiseaseRecordDetailForResolvedRecord(diseaseRecord);
    }

    private DiseaseRecordDetailResponse getDiseaseRecordDetailForResolvedRecord(DiseaseRecord diseaseRecord) {
        Integer id = diseaseRecord.getId();
        List<DiseaseTreatmentResponse> treatments = diseaseTreatmentRepository
                .findAllByDiseaseRecord_IdOrderByTreatedAtDescIdDesc(id)
                .stream()
                .map(diseaseRecordMapper::toDiseaseTreatmentResponse)
                .toList();

        Long treatmentCount = diseaseTreatmentRepository.countByDiseaseRecord_Id(id);
        LocalDateTime latestTreatmentAt = diseaseTreatmentRepository.findLatestTreatedAtByDiseaseRecordId(id);
        BigDecimal totalTreatmentCost = diseaseTreatmentRepository.sumCostAmountByDiseaseRecordId(id);

        return diseaseRecordMapper.toDiseaseRecordDetailResponse(
                diseaseRecord,
                treatments,
                treatmentCount,
                latestTreatmentAt,
                totalTreatmentCost);
    }

    public DiseaseRecordResponse createDiseaseRecord(Integer seasonId, CreateDiseaseRecordRequest request) {
        Season season = getSeasonForCurrentFarmer(seasonId);
        return createDiseaseRecordForResolvedSeason(season, request);
    }

    public DiseaseRecordResponse createDiseaseRecordForAssignedEmployee(Integer seasonId, CreateDiseaseRecordRequest request) {
        Season season = getSeasonForCurrentEmployee(seasonId);
        return createDiseaseRecordForResolvedSeason(season, request);
    }

    private DiseaseRecordResponse createDiseaseRecordForResolvedSeason(Season season, CreateDiseaseRecordRequest request) {
        ensureSeasonOpenForDiseaseWrite(season, true);
        validateDetectedAtWithinSeason(season, request.getDetectedAt());

        DiseaseSeverity diseaseSeverity = parseSeverity(request.getSeverity());
        DiseaseStatus diseaseStatus = StringUtils.hasText(request.getStatus())
                ? parseStatus(request.getStatus())
                : DiseaseStatus.OPEN;

        if (!StringUtils.hasText(request.getDiseaseName())) {
            throw new AppException(ErrorCode.KEY_INVALID);
        }

        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = seasonWorkspaceAccessService.getCurrentUser();

        Integer resolvedIncidentId = null;
        if (request.getIncidentId() != null) {
            Incident incident = resolveIncidentForSeason(request.getIncidentId(), season.getId());
            resolvedIncidentId = incident.getId();
        }

        DiseaseRecord diseaseRecord = DiseaseRecord.builder()
                .season(season)
                .plotId(season.getPlot() != null ? season.getPlot().getId() : null)
                .cropId(season.getCrop() != null ? season.getCrop().getId() : null)
                .varietyId(season.getVariety() != null ? season.getVariety().getId() : null)
                .reportedByUserId(currentUser.getId())
                .incidentId(resolvedIncidentId)
                .diseaseName(request.getDiseaseName().trim())
                .symptomSummary(trimToNull(request.getSymptomSummary()))
                .severity(diseaseSeverity)
                .status(diseaseStatus)
                .detectedAt(request.getDetectedAt())
                .affectedPlantCount(request.getAffectedPlantCount())
                .affectedAreaValue(request.getAffectedAreaValue())
                .affectedAreaUnit(trimToNull(request.getAffectedAreaUnit()))
                .evidenceUrl(trimToNull(request.getEvidenceUrl()))
                .notes(trimToNull(request.getNotes()))
                .build();

        DiseaseRecord saved = diseaseRecordRepository.save(diseaseRecord);
        logRecordAudit("DISEASE_RECORD_CREATED", saved);
        return diseaseRecordMapper.toDiseaseRecordResponse(saved, 0L, null);
    }

    public DiseaseRecordResponse updateDiseaseRecord(Integer id, UpdateDiseaseRecordRequest request) {
        DiseaseRecord diseaseRecord = getDiseaseRecordForCurrentFarmer(id);
        return updateResolvedDiseaseRecord(diseaseRecord, request);
    }

    public DiseaseRecordResponse updateDiseaseRecordForAssignedEmployee(Integer id, UpdateDiseaseRecordRequest request) {
        DiseaseRecord diseaseRecord = getDiseaseRecordForCurrentEmployee(id);
        seasonWorkspaceAccessService.assertCurrentUserCanManageRecord(
                resolveSeasonOfRecord(diseaseRecord),
                diseaseRecord.getReportedByUserId());
        return updateResolvedDiseaseRecord(diseaseRecord, request);
    }

    private DiseaseRecordResponse updateResolvedDiseaseRecord(DiseaseRecord diseaseRecord, UpdateDiseaseRecordRequest request) {
        Season season = resolveSeasonOfRecord(diseaseRecord);
        ensureSeasonOpenForDiseaseWrite(season, false);

        if (request.getDiseaseName() != null) {
            if (!StringUtils.hasText(request.getDiseaseName())) {
                throw new AppException(ErrorCode.KEY_INVALID);
            }
            diseaseRecord.setDiseaseName(request.getDiseaseName().trim());
        }

        if (request.getSymptomSummary() != null) {
            diseaseRecord.setSymptomSummary(trimToNull(request.getSymptomSummary()));
        }

        if (request.getSeverity() != null) {
            diseaseRecord.setSeverity(parseSeverity(request.getSeverity()));
        }

        if (request.getStatus() != null) {
            diseaseRecord.setStatus(parseStatus(request.getStatus()));
        }

        if (request.getDetectedAt() != null) {
            validateDetectedAtWithinSeason(season, request.getDetectedAt());
            diseaseRecord.setDetectedAt(request.getDetectedAt());
        }

        if (request.getAffectedPlantCount() != null) {
            diseaseRecord.setAffectedPlantCount(request.getAffectedPlantCount());
        }

        if (request.getAffectedAreaValue() != null) {
            diseaseRecord.setAffectedAreaValue(request.getAffectedAreaValue());
        }

        if (request.getAffectedAreaUnit() != null) {
            diseaseRecord.setAffectedAreaUnit(trimToNull(request.getAffectedAreaUnit()));
        }

        if (request.getEvidenceUrl() != null) {
            diseaseRecord.setEvidenceUrl(trimToNull(request.getEvidenceUrl()));
        }

        if (request.getNotes() != null) {
            diseaseRecord.setNotes(trimToNull(request.getNotes()));
        }

        if (request.getIncidentId() != null) {
            Incident incident = resolveIncidentForSeason(request.getIncidentId(), season.getId());
            diseaseRecord.setIncidentId(incident.getId());
        }

        DiseaseRecord saved = diseaseRecordRepository.save(diseaseRecord);
        logRecordAudit("DISEASE_RECORD_UPDATED", saved);

        long treatmentCount = diseaseTreatmentRepository.countByDiseaseRecord_Id(saved.getId());
        LocalDateTime latestTreatmentAt = diseaseTreatmentRepository.findLatestTreatedAtByDiseaseRecordId(saved.getId());
        return diseaseRecordMapper.toDiseaseRecordResponse(saved, treatmentCount, latestTreatmentAt);
    }

    public void deleteDiseaseRecord(Integer id) {
        DiseaseRecord diseaseRecord = getDiseaseRecordForCurrentFarmer(id);
        deleteResolvedDiseaseRecord(diseaseRecord);
    }

    public void deleteDiseaseRecordForAssignedEmployee(Integer id) {
        DiseaseRecord diseaseRecord = getDiseaseRecordForCurrentEmployee(id);
        seasonWorkspaceAccessService.assertCurrentUserCanManageRecord(
                resolveSeasonOfRecord(diseaseRecord),
                diseaseRecord.getReportedByUserId());
        deleteResolvedDiseaseRecord(diseaseRecord);
    }

    private void deleteResolvedDiseaseRecord(DiseaseRecord diseaseRecord) {
        Season season = resolveSeasonOfRecord(diseaseRecord);
        ensureSeasonOpenForDiseaseWrite(season, false);

        diseaseTreatmentRepository.deleteAllByDiseaseRecord_Id(diseaseRecord.getId());
        diseaseRecordRepository.delete(diseaseRecord);
        logRecordAudit("DISEASE_RECORD_DELETED", diseaseRecord);
    }

    @Transactional(readOnly = true)
    public PageResponse<DiseaseTreatmentResponse> listTreatments(Integer diseaseRecordId, int page, int size) {
        getDiseaseRecordForCurrentFarmer(diseaseRecordId);
        return listTreatmentsForResolvedRecord(diseaseRecordId, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<DiseaseTreatmentResponse> listTreatmentsForAssignedEmployee(Integer diseaseRecordId, int page, int size) {
        getDiseaseRecordForCurrentEmployee(diseaseRecordId);
        return listTreatmentsForResolvedRecord(diseaseRecordId, page, size);
    }

    private PageResponse<DiseaseTreatmentResponse> listTreatmentsForResolvedRecord(Integer diseaseRecordId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("treatedAt"), Sort.Order.desc("id")));
        Page<DiseaseTreatment> treatmentPage = diseaseTreatmentRepository.findAllByDiseaseRecord_Id(diseaseRecordId, pageable);

        List<DiseaseTreatmentResponse> items = treatmentPage.getContent()
                .stream()
                .map(diseaseRecordMapper::toDiseaseTreatmentResponse)
                .toList();

        return PageResponse.of(treatmentPage, items);
    }

    public DiseaseTreatmentResponse createTreatment(Integer diseaseRecordId, CreateDiseaseTreatmentRequest request) {
        DiseaseRecord diseaseRecord = getDiseaseRecordForCurrentFarmer(diseaseRecordId);
        return createTreatmentForResolvedRecord(diseaseRecord, request);
    }

    public DiseaseTreatmentResponse createTreatmentForAssignedEmployee(Integer diseaseRecordId, CreateDiseaseTreatmentRequest request) {
        DiseaseRecord diseaseRecord = getDiseaseRecordForCurrentEmployee(diseaseRecordId);
        return createTreatmentForResolvedRecord(diseaseRecord, request);
    }

    private DiseaseTreatmentResponse createTreatmentForResolvedRecord(
            DiseaseRecord diseaseRecord,
            CreateDiseaseTreatmentRequest request) {
        Season season = resolveSeasonOfRecord(diseaseRecord);
        ensureSeasonOpenForDiseaseWrite(season, false);
        validateTreatmentDate(diseaseRecord, request.getTreatedAt());

        if (!StringUtils.hasText(request.getMethod())) {
            throw new AppException(ErrorCode.KEY_INVALID);
        }

        ResolvedSupplyReference resolvedSupplyReference = resolveSupplyReference(
                request.getSupplyItemId(),
                request.getSupplyLotId(),
                season);
        Expense expense = resolveExpenseForRecordSeason(request.getExpenseId(), diseaseRecord);

        TreatmentEffectiveness effectiveness = null;
        if (StringUtils.hasText(request.getEffectiveness())) {
            effectiveness = parseEffectiveness(request.getEffectiveness());
        }

        org.example.QuanLyMuaVu.module.identity.entity.User currentUser = seasonWorkspaceAccessService.getCurrentUser();

        DiseaseTreatment treatment = DiseaseTreatment.builder()
                .diseaseRecord(diseaseRecord)
                .treatedAt(request.getTreatedAt())
                .method(request.getMethod().trim())
                .supplyItemId(resolvedSupplyReference.supplyItemId())
                .supplyLotId(resolvedSupplyReference.supplyLotId())
                .materialName(trimToNull(request.getMaterialName()))
                .quantityUsed(request.getQuantityUsed())
                .unit(trimToNull(request.getUnit()))
                .costAmount(request.getCostAmount())
                .expenseId(expense != null ? expense.getId() : null)
                .effectiveness(effectiveness)
                .resultSummary(trimToNull(request.getResultSummary()))
                .nextReviewAt(request.getNextReviewAt())
                .notes(trimToNull(request.getNotes()))
                .createdByUserId(currentUser.getId())
                .build();

        DiseaseTreatment saved = diseaseTreatmentRepository.save(treatment);
        logTreatmentAudit("DISEASE_TREATMENT_CREATED", saved);
        return diseaseRecordMapper.toDiseaseTreatmentResponse(saved);
    }

    public DiseaseTreatmentResponse updateTreatment(Integer id, UpdateDiseaseTreatmentRequest request) {
        DiseaseTreatment treatment = getDiseaseTreatmentForCurrentFarmer(id);
        return updateResolvedTreatment(treatment, request);
    }

    public DiseaseTreatmentResponse updateTreatmentForAssignedEmployee(Integer id, UpdateDiseaseTreatmentRequest request) {
        DiseaseTreatment treatment = getDiseaseTreatmentForCurrentEmployee(id);
        seasonWorkspaceAccessService.assertCurrentUserCanManageRecord(
                resolveSeasonOfRecord(treatment.getDiseaseRecord()),
                treatment.getCreatedByUserId());
        return updateResolvedTreatment(treatment, request);
    }

    private DiseaseTreatmentResponse updateResolvedTreatment(DiseaseTreatment treatment, UpdateDiseaseTreatmentRequest request) {
        DiseaseRecord diseaseRecord = treatment.getDiseaseRecord();
        Season season = resolveSeasonOfRecord(diseaseRecord);
        ensureSeasonOpenForDiseaseWrite(season, false);

        if (request.getTreatedAt() != null) {
            validateTreatmentDate(diseaseRecord, request.getTreatedAt());
            treatment.setTreatedAt(request.getTreatedAt());
        }

        if (request.getMethod() != null) {
            if (!StringUtils.hasText(request.getMethod())) {
                throw new AppException(ErrorCode.KEY_INVALID);
            }
            treatment.setMethod(request.getMethod().trim());
        }

        if (request.getSupplyItemId() != null || request.getSupplyLotId() != null) {
            Integer targetSupplyItemId = request.getSupplyItemId() != null
                    ? request.getSupplyItemId()
                    : treatment.getSupplyItemId();
            Integer targetSupplyLotId = request.getSupplyLotId() != null
                    ? request.getSupplyLotId()
                    : treatment.getSupplyLotId();

            ResolvedSupplyReference resolvedSupplyReference = resolveSupplyReference(targetSupplyItemId, targetSupplyLotId, season);
            treatment.setSupplyItemId(resolvedSupplyReference.supplyItemId());
            treatment.setSupplyLotId(resolvedSupplyReference.supplyLotId());
        }

        if (request.getMaterialName() != null) {
            treatment.setMaterialName(trimToNull(request.getMaterialName()));
        }

        if (request.getQuantityUsed() != null) {
            treatment.setQuantityUsed(request.getQuantityUsed());
        }

        if (request.getUnit() != null) {
            treatment.setUnit(trimToNull(request.getUnit()));
        }

        if (request.getCostAmount() != null) {
            treatment.setCostAmount(request.getCostAmount());
        }

        if (request.getExpenseId() != null) {
            Expense expense = resolveExpenseForRecordSeason(request.getExpenseId(), diseaseRecord);
            treatment.setExpenseId(expense != null ? expense.getId() : null);
        }

        if (request.getEffectiveness() != null) {
            treatment.setEffectiveness(parseEffectiveness(request.getEffectiveness()));
        }

        if (request.getResultSummary() != null) {
            treatment.setResultSummary(trimToNull(request.getResultSummary()));
        }

        if (request.getNextReviewAt() != null) {
            treatment.setNextReviewAt(request.getNextReviewAt());
        }

        if (request.getNotes() != null) {
            treatment.setNotes(trimToNull(request.getNotes()));
        }

        DiseaseTreatment saved = diseaseTreatmentRepository.save(treatment);
        logTreatmentAudit("DISEASE_TREATMENT_UPDATED", saved);
        return diseaseRecordMapper.toDiseaseTreatmentResponse(saved);
    }

    public void deleteTreatment(Integer id) {
        DiseaseTreatment treatment = getDiseaseTreatmentForCurrentFarmer(id);
        deleteResolvedTreatment(treatment);
    }

    public void deleteTreatmentForAssignedEmployee(Integer id) {
        DiseaseTreatment treatment = getDiseaseTreatmentForCurrentEmployee(id);
        seasonWorkspaceAccessService.assertCurrentUserCanManageRecord(
                resolveSeasonOfRecord(treatment.getDiseaseRecord()),
                treatment.getCreatedByUserId());
        deleteResolvedTreatment(treatment);
    }

    private void deleteResolvedTreatment(DiseaseTreatment treatment) {
        Season season = resolveSeasonOfRecord(treatment.getDiseaseRecord());
        ensureSeasonOpenForDiseaseWrite(season, false);
        diseaseTreatmentRepository.delete(treatment);
        logTreatmentAudit("DISEASE_TREATMENT_DELETED", treatment);
    }

    private Specification<DiseaseRecord> buildRecordSpecification(
            Integer seasonId,
            String status,
            String severity,
            String q,
            LocalDate fromDetectedAt,
            LocalDate toDetectedAt) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("season").get("id"), seasonId));

            if (StringUtils.hasText(status)) {
                DiseaseStatus diseaseStatus = parseStatus(status);
                predicates.add(cb.equal(root.get("status"), diseaseStatus));
            }

            if (StringUtils.hasText(severity)) {
                DiseaseSeverity diseaseSeverity = parseSeverity(severity);
                predicates.add(cb.equal(root.get("severity"), diseaseSeverity));
            }

            if (StringUtils.hasText(q)) {
                String search = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate matchName = cb.like(cb.lower(root.get("diseaseName")), search);
                Predicate matchSymptom = cb.like(cb.lower(cb.coalesce(root.get("symptomSummary"), "")), search);
                predicates.add(cb.or(matchName, matchSymptom));
            }

            if (fromDetectedAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("detectedAt"), fromDetectedAt.atStartOfDay()));
            }
            if (toDetectedAt != null) {
                predicates.add(cb.lessThan(root.get("detectedAt"), toDetectedAt.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DiseaseRecord getDiseaseRecordForCurrentFarmer(Integer id) {
        DiseaseRecord diseaseRecord = diseaseRecordRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISEASE_RECORD_NOT_FOUND));
        Season season = resolveSeasonOfRecord(diseaseRecord);
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return diseaseRecord;
    }

    private DiseaseRecord getDiseaseRecordForCurrentEmployee(Integer id) {
        DiseaseRecord diseaseRecord = diseaseRecordRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISEASE_RECORD_NOT_FOUND));
        Season season = resolveSeasonOfRecord(diseaseRecord);
        seasonWorkspaceAccessService.requireActiveEmployeeAssignment(season);
        return diseaseRecord;
    }

    private DiseaseTreatment getDiseaseTreatmentForCurrentFarmer(Integer id) {
        DiseaseTreatment treatment = diseaseTreatmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISEASE_TREATMENT_NOT_FOUND));
        DiseaseRecord diseaseRecord = treatment.getDiseaseRecord();
        if (diseaseRecord == null) {
            throw new AppException(ErrorCode.DISEASE_RECORD_NOT_FOUND);
        }
        Season season = resolveSeasonOfRecord(diseaseRecord);
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return treatment;
    }

    private DiseaseTreatment getDiseaseTreatmentForCurrentEmployee(Integer id) {
        DiseaseTreatment treatment = diseaseTreatmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DISEASE_TREATMENT_NOT_FOUND));
        DiseaseRecord diseaseRecord = treatment.getDiseaseRecord();
        if (diseaseRecord == null) {
            throw new AppException(ErrorCode.DISEASE_RECORD_NOT_FOUND);
        }
        Season season = resolveSeasonOfRecord(diseaseRecord);
        seasonWorkspaceAccessService.requireActiveEmployeeAssignment(season);
        return treatment;
    }

    private Season getSeasonForCurrentFarmer(Integer seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        farmAccessService.assertCurrentUserCanAccessSeason(season);
        return season;
    }

    private Season getSeasonForCurrentEmployee(Integer seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        seasonWorkspaceAccessService.requireActiveEmployeeAssignment(season);
        return season;
    }

    private Season resolveSeasonOfRecord(DiseaseRecord diseaseRecord) {
        Season season = diseaseRecord.getSeason();
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        return season;
    }

    private void ensureSeasonOpenForDiseaseWrite(Season season, boolean forCreate) {
        if (season == null || season.getStatus() == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            if (forCreate) {
                throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_ADD_DISEASE_RECORD);
            }
            throw new AppException(ErrorCode.SEASON_CLOSED_CANNOT_MODIFY_DISEASE_RECORD);
        }
    }

    private void validateDetectedAtWithinSeason(Season season, LocalDateTime detectedAt) {
        if (detectedAt == null) {
            throw new AppException(ErrorCode.KEY_INVALID);
        }
        LocalDate startDate = season.getStartDate();
        LocalDate endDate = season.getEndDate() != null ? season.getEndDate() : season.getPlannedHarvestDate();
        LocalDate targetDate = detectedAt.toLocalDate();

        if (startDate == null || targetDate.isBefore(startDate) || (endDate != null && targetDate.isAfter(endDate))) {
            throw new AppException(ErrorCode.INVALID_DISEASE_DETECTED_AT);
        }
    }

    private void validateTreatmentDate(DiseaseRecord diseaseRecord, LocalDateTime treatedAt) {
        if (treatedAt == null) {
            throw new AppException(ErrorCode.KEY_INVALID);
        }
        if (diseaseRecord.getDetectedAt() != null && treatedAt.isBefore(diseaseRecord.getDetectedAt())) {
            throw new AppException(ErrorCode.INVALID_DISEASE_TREATED_AT);
        }
    }

    private DiseaseSeverity parseSeverity(String severity) {
        try {
            return DiseaseSeverity.fromCode(severity);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_DISEASE_SEVERITY);
        }
    }

    private DiseaseStatus parseStatus(String status) {
        try {
            return DiseaseStatus.fromCode(status);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_DISEASE_STATUS);
        }
    }

    private TreatmentEffectiveness parseEffectiveness(String effectiveness) {
        try {
            return TreatmentEffectiveness.fromCode(effectiveness);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_TREATMENT_EFFECTIVENESS);
        }
    }

    private Incident resolveIncidentForSeason(Integer incidentId, Integer seasonId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));
        Integer incidentSeasonId = incident.getSeasonId() != null
                ? incident.getSeasonId()
                : incident.getSeason() != null ? incident.getSeason().getId() : null;
        if (incidentSeasonId == null || !incidentSeasonId.equals(seasonId)) {
            throw new AppException(ErrorCode.DISEASE_REFERENCE_SEASON_MISMATCH);
        }
        return incident;
    }

    private Expense resolveExpenseForRecordSeason(Integer expenseId, DiseaseRecord diseaseRecord) {
        if (expenseId == null) {
            return null;
        }
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPENSE_NOT_FOUND));
        Season expenseSeason = resolveExpenseSeason(expense);
        if (expenseSeason == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        farmAccessService.assertCurrentUserCanAccessSeason(expenseSeason);

        Integer diseaseSeasonId = diseaseRecord.getSeason() != null ? diseaseRecord.getSeason().getId() : null;
        if (diseaseSeasonId == null || !diseaseSeasonId.equals(expenseSeason.getId())) {
            throw new AppException(ErrorCode.DISEASE_REFERENCE_SEASON_MISMATCH);
        }
        return expense;
    }

    private Season resolveExpenseSeason(Expense expense) {
        if (expense == null) {
            return null;
        }
        if (expense.getSeason() != null) {
            return expense.getSeason();
        }
        Integer seasonId = expense.getSeasonId();
        if (seasonId == null) {
            return null;
        }
        return seasonRepository.findById(seasonId).orElse(null);
    }

    private ResolvedSupplyReference resolveSupplyReference(Integer supplyItemId, Integer supplyLotId, Season season) {
        Integer resolvedSupplyItemId = supplyItemId;
        Integer resolvedSupplyLotId = supplyLotId;

        if (resolvedSupplyLotId != null) {
            SupplyLot supplyLot = supplyLotRepository.findById(resolvedSupplyLotId)
                    .orElseThrow(() -> new AppException(ErrorCode.SUPPLY_LOT_NOT_FOUND));
            ensureSupplyLotAccessibleForCurrentWorkspace(supplyLot.getId(), season);

            Integer lotSupplyItemId = supplyLot.getSupplyItem() != null ? supplyLot.getSupplyItem().getId() : null;
            if (resolvedSupplyItemId != null && lotSupplyItemId != null && !resolvedSupplyItemId.equals(lotSupplyItemId)) {
                throw new AppException(ErrorCode.DISEASE_SUPPLY_ITEM_LOT_MISMATCH);
            }
            if (resolvedSupplyItemId == null) {
                resolvedSupplyItemId = lotSupplyItemId;
            }
        }

        if (resolvedSupplyItemId != null) {
            if (!supplyItemRepository.existsById(resolvedSupplyItemId)) {
                throw new AppException(ErrorCode.SUPPLY_ITEM_NOT_FOUND);
            }
            if (resolvedSupplyLotId == null && !seasonWorkspaceAccessService.isCurrentUserSeasonOwner(season)) {
                ensureSupplyItemAccessibleForCurrentWorkspace(resolvedSupplyItemId, season);
            }
        }

        return new ResolvedSupplyReference(resolvedSupplyItemId, resolvedSupplyLotId);
    }

    private void ensureSupplyItemAccessibleForCurrentWorkspace(Integer supplyItemId, Season season) {
        Integer farmId = seasonWorkspaceAccessService.resolveSeasonFarmId(season);
        List<Integer> accessibleFarmIds = farmId != null ? List.of(farmId) : farmAccessService.getAccessibleFarmIdsForCurrentUser();
        if (accessibleFarmIds == null || accessibleFarmIds.isEmpty()) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        boolean hasPositiveBalance = inventoryBalanceRepository
                .existsBySupplyLot_SupplyItem_IdAndWarehouse_Farm_IdInAndQuantityGreaterThan(
                        supplyItemId,
                        accessibleFarmIds,
                        BigDecimal.ZERO);
        if (!hasPositiveBalance) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private void ensureSupplyLotAccessibleForCurrentWorkspace(Integer supplyLotId, Season season) {
        Integer farmId = seasonWorkspaceAccessService.resolveSeasonFarmId(season);
        List<Integer> accessibleFarmIds = farmId != null ? List.of(farmId) : farmAccessService.getAccessibleFarmIdsForCurrentUser();
        if (accessibleFarmIds == null || accessibleFarmIds.isEmpty()) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        boolean hasBalance = inventoryBalanceRepository
                .existsBySupplyLot_IdAndWarehouse_Farm_IdIn(supplyLotId, accessibleFarmIds);
        if (hasBalance) {
            return;
        }

        boolean hasMovements = stockMovementRepository
                .existsBySupplyLot_IdAndWarehouse_Farm_IdIn(supplyLotId, accessibleFarmIds);
        if (hasMovements) {
            return;
        }

        throw new AppException(ErrorCode.FORBIDDEN);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void logRecordAudit(String operation, DiseaseRecord diseaseRecord) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("diseaseRecordId", diseaseRecord.getId());
        snapshot.put("seasonId", diseaseRecord.getSeason() != null ? diseaseRecord.getSeason().getId() : null);
        snapshot.put("status", diseaseRecord.getStatus() != null ? diseaseRecord.getStatus().name() : null);
        snapshot.put("severity", diseaseRecord.getSeverity() != null ? diseaseRecord.getSeverity().name() : null);
        snapshot.put("detectedAt", diseaseRecord.getDetectedAt());
        snapshot.put("incidentId", diseaseRecord.getIncidentId());

        String actor = resolveAuditActor();
        auditLogService.logModuleOperation(
                "DISEASE",
                "DISEASE_RECORD",
                diseaseRecord.getId(),
                operation,
                actor,
                snapshot,
                null,
                null);
    }

    private void logTreatmentAudit(String operation, DiseaseTreatment treatment) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("diseaseTreatmentId", treatment.getId());
        snapshot.put("diseaseRecordId", treatment.getDiseaseRecord() != null ? treatment.getDiseaseRecord().getId() : null);
        snapshot.put("treatedAt", treatment.getTreatedAt());
        snapshot.put("supplyItemId", treatment.getSupplyItemId());
        snapshot.put("supplyLotId", treatment.getSupplyLotId());
        snapshot.put("expenseId", treatment.getExpenseId());
        snapshot.put("effectiveness", treatment.getEffectiveness() != null ? treatment.getEffectiveness().name() : null);

        String actor = resolveAuditActor();
        auditLogService.logModuleOperation(
                "DISEASE",
                "DISEASE_TREATMENT",
                treatment.getId(),
                operation,
                actor,
                snapshot,
                null,
                null);
    }

    private String resolveAuditActor() {
        try {
            org.example.QuanLyMuaVu.module.identity.entity.User actor = farmAccessService.getCurrentUser();
            if (actor != null && StringUtils.hasText(actor.getUsername())) {
                return actor.getUsername();
            }
        } catch (Exception ignored) {
            // Keep fallback.
        }
        return "system";
    }

    private record ResolvedSupplyReference(Integer supplyItemId, Integer supplyLotId) {
    }
}
