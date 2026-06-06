package org.example.QuanLyMuaVu.module.ai.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.IncidentStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.ai.dto.request.DiseaseSuggestionRequest;
import org.example.QuanLyMuaVu.module.ai.dto.response.DiseaseSuggestionResponse;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.incident.entity.Incident;
import org.example.QuanLyMuaVu.module.incident.repository.IncidentRepository;
import org.example.QuanLyMuaVu.module.inventory.entity.InventoryBalance;
import org.example.QuanLyMuaVu.module.inventory.entity.SupplyLot;
import org.example.QuanLyMuaVu.module.inventory.repository.InventoryBalanceRepository;
import org.example.QuanLyMuaVu.module.season.entity.DiseaseRecord;
import org.example.QuanLyMuaVu.module.season.entity.DiseaseTreatment;
import org.example.QuanLyMuaVu.module.season.entity.FieldLog;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.repository.DiseaseRecordRepository;
import org.example.QuanLyMuaVu.module.season.repository.DiseaseTreatmentRepository;
import org.example.QuanLyMuaVu.module.season.repository.FieldLogRepository;
import org.example.QuanLyMuaVu.module.season.service.SeasonWorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class DiseaseSuggestionService {

    static final int MAX_TREATMENTS = 10;
    static final int MAX_FIELD_LOGS = 10;
    static final int MAX_INCIDENTS = 5;
    static final int MAX_INVENTORY_ROWS = 12;
    static final int MAX_TEXT_LENGTH = 240;
    static final String DEFAULT_QUESTION =
            "Hay goi y huong xu ly tham khao cho tinh trang benh nay dua tren du lieu hien co.";
    static final String WARNING_MESSAGE =
            "AI chi ho tro quyet dinh tham khao, khong thay the chuyen gia nong nghiep va khong tu dong tao treatment/xuat kho/expense.";

    GeminiService geminiService;
    DiseaseRecordRepository diseaseRecordRepository;
    DiseaseTreatmentRepository diseaseTreatmentRepository;
    FieldLogRepository fieldLogRepository;
    IncidentRepository incidentRepository;
    InventoryBalanceRepository inventoryBalanceRepository;
    FarmAccessPort farmAccessService;
    SeasonWorkspaceAccessService seasonWorkspaceAccessService;
    AuditLogService auditLogService;

    public DiseaseSuggestionResponse generateSuggestion(Integer diseaseRecordId, DiseaseSuggestionRequest request) {
        if (diseaseRecordId == null) {
            throw new AppException(ErrorCode.KEY_INVALID);
        }

        DiseaseRecord diseaseRecord = diseaseRecordRepository.findById(diseaseRecordId)
                .orElseThrow(() -> new AppException(ErrorCode.DISEASE_RECORD_NOT_FOUND));
        Season season = diseaseRecord.getSeason();
        if (season == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        seasonWorkspaceAccessService.assertCurrentUserCanAccessSeason(season);

        boolean includeInventory = request == null
                || request.getIncludeInventory() == null
                || request.getIncludeInventory();
        String question = normalizeText(request != null ? request.getQuestion() : null);
        String additionalNote = normalizeText(request != null ? request.getAdditionalNote() : null);
        if (!StringUtils.hasText(question)) {
            question = DEFAULT_QUESTION;
        }

        List<DiseaseTreatment> treatments = diseaseTreatmentRepository
                .findAllByDiseaseRecord_IdOrderByTreatedAtDescIdDesc(diseaseRecordId)
                .stream()
                .limit(MAX_TREATMENTS)
                .toList();
        List<FieldLog> fieldLogs = fieldLogRepository
                .findTop10BySeason_IdOrderByLogDateDescIdDesc(season.getId())
                .stream()
                .limit(MAX_FIELD_LOGS)
                .toList();
        List<Incident> incidents = incidentRepository
                .findTop5BySeason_IdOrderByCreatedAtDescIdDesc(season.getId())
                .stream()
                .limit(MAX_INCIDENTS)
                .toList();

        long openIncidentCount = incidentRepository.countBySeasonIdAndStatusIn(
                season.getId(),
                List.of(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS));

        Incident linkedIncident = null;
        if (diseaseRecord.getIncidentId() != null) {
            linkedIncident = incidentRepository.findById(diseaseRecord.getIncidentId()).orElse(null);
        }

        List<InventoryBalance> inventoryRows = loadInventoryRows(includeInventory, season);
        String structuredContext = buildStructuredContext(
                diseaseRecord,
                season,
                treatments,
                fieldLogs,
                linkedIncident,
                incidents,
                openIncidentCount,
                inventoryRows,
                includeInventory,
                additionalNote);
        String instruction = buildInstruction(question, includeInventory, inventoryRows.isEmpty());
        String suggestionText = geminiService.chatAsAgriculturalExpert(instruction, structuredContext);

        LocalDateTime generatedAt = LocalDateTime.now();
        Map<String, Object> usedContextSummary = buildContextSummary(
                diseaseRecord,
                season,
                treatments,
                fieldLogs,
                incidents,
                includeInventory,
                inventoryRows,
                additionalNote,
                question);

        logSuggestionAudit(diseaseRecordId, usedContextSummary, generatedAt);

        return DiseaseSuggestionResponse.builder()
                .diseaseRecordId(diseaseRecordId)
                .suggestionText(suggestionText)
                .usedContextSummary(usedContextSummary)
                .generatedAt(generatedAt)
                .warning(WARNING_MESSAGE)
                .build();
    }

    private List<InventoryBalance> loadInventoryRows(boolean includeInventory, Season season) {
        if (!includeInventory) {
            return List.of();
        }

        Integer seasonFarmId = seasonWorkspaceAccessService.resolveSeasonFarmId(season);
        List<Integer> accessibleFarmIds = seasonFarmId != null
                ? List.of(seasonFarmId)
                : farmAccessService.getAccessibleFarmIdsForCurrentUser();
        if (accessibleFarmIds.isEmpty()) {
            return List.of();
        }

        return inventoryBalanceRepository.findPositiveByFarmIdsWithDetails(accessibleFarmIds)
                .stream()
                .limit(MAX_INVENTORY_ROWS)
                .toList();
    }

    private String buildInstruction(String question, boolean includeInventory, boolean inventoryEmpty) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cau hoi cua nong dan: ").append(question).append("\n\n");
        sb.append("Yeu cau tra loi dung dinh dang sau:\n");
        sb.append("a. Tom tat tinh trang\n");
        sb.append("b. Du lieu con thieu\n");
        sb.append("c. Huong xu ly tham khao\n");
        sb.append("d. Vat tu hien co co the can nhac\n");
        sb.append("e. Rui ro/canh bao\n");
        sb.append("f. Buoc tiep theo nen ghi nhan trong he thong\n\n");
        sb.append("Rang buoc an toan bat buoc:\n");
        sb.append("- Day chi la ho tro quyet dinh, khong phai chan doan chac chan.\n");
        sb.append("- Khong duoc dung cum tuong duong voi 'chac chan benh la...'.\n");
        sb.append("- Khong thay the chuyen gia nong nghiep.\n");
        sb.append("- Khong de xuat hanh dong tu dong tren he thong.\n");
        sb.append("- Khong tu dong tao treatment.\n");
        sb.append("- Khong tu dong tru kho.\n");
        sb.append("- Khong tu dong tao expense.\n");
        sb.append("- Neu thieu du lieu, phai noi ro can bo sung thong tin gi.\n");
        if (!includeInventory) {
            sb.append("- Inventory khong duoc yeu cau, khong goi y ten vat tu cu the.\n");
        } else if (inventoryEmpty) {
            sb.append("- Khong co du lieu ton kho noi bo, khong goi y thuoc/vat tu ngoai du lieu noi bo.\n");
        } else {
            sb.append("- Chi goi y vat tu nam trong danh sach ton kho duoc cung cap.\n");
        }
        sb.append("Bat buoc ket thuc bang mot dong disclaimer: ");
        sb.append("'Khuyen nghi nay chi mang tinh tham khao, vui long tham van chuyen gia nong nghiep truoc khi ap dung.'");
        return sb.toString();
    }

    private String buildStructuredContext(
            DiseaseRecord diseaseRecord,
            Season season,
            List<DiseaseTreatment> treatments,
            List<FieldLog> fieldLogs,
            Incident linkedIncident,
            List<Incident> incidents,
            long openIncidentCount,
            List<InventoryBalance> inventoryRows,
            boolean includeInventory,
            String additionalNote) {
        StringBuilder sb = new StringBuilder();
        sb.append("Du lieu noi bo lien quan ban ghi benh:\n");
        sb.append("- DiseaseRecordId: ").append(diseaseRecord.getId()).append("\n");
        sb.append("- DiseaseName: ").append(safeText(diseaseRecord.getDiseaseName())).append("\n");
        sb.append("- Severity: ").append(diseaseRecord.getSeverity()).append("\n");
        sb.append("- Status: ").append(diseaseRecord.getStatus()).append("\n");
        sb.append("- DetectedAt: ").append(diseaseRecord.getDetectedAt()).append("\n");
        sb.append("- SymptomSummary: ").append(safeText(diseaseRecord.getSymptomSummary())).append("\n");
        sb.append("- AffectedPlantCount: ").append(diseaseRecord.getAffectedPlantCount()).append("\n");
        sb.append("- AffectedArea: ").append(diseaseRecord.getAffectedAreaValue()).append(" ")
                .append(safeText(diseaseRecord.getAffectedAreaUnit())).append("\n");
        sb.append("- RecordNotes: ").append(safeText(diseaseRecord.getNotes())).append("\n\n");

        sb.append("Context mua vu:\n");
        sb.append("- SeasonId: ").append(season.getId()).append("\n");
        sb.append("- SeasonName: ").append(safeText(season.getSeasonName())).append("\n");
        sb.append("- SeasonStatus: ").append(season.getStatus()).append("\n");
        sb.append("- StartDate: ").append(season.getStartDate()).append("\n");
        sb.append("- EndDate: ").append(season.getEndDate()).append("\n");
        sb.append("- PlannedHarvestDate: ").append(season.getPlannedHarvestDate()).append("\n");
        sb.append("- Plot: ").append(season.getPlot() != null ? safeText(season.getPlot().getPlotName()) : "N/A").append("\n");
        sb.append("- Crop: ").append(season.getCrop() != null ? safeText(season.getCrop().getCropName()) : "N/A").append("\n");
        sb.append("- Variety: ").append(season.getVariety() != null ? safeText(season.getVariety().getName()) : "N/A").append("\n\n");

        sb.append("Lich su dieu tri gan day (toi da ").append(MAX_TREATMENTS).append("):\n");
        if (treatments.isEmpty()) {
            sb.append("- Chua co lich su dieu tri.\n");
        } else {
            for (DiseaseTreatment treatment : treatments) {
                sb.append("- ")
                        .append(treatment.getTreatedAt())
                        .append(" | method=").append(safeText(treatment.getMethod()))
                        .append(" | material=").append(resolveTreatmentMaterialName(treatment))
                        .append(" | qty=").append(treatment.getQuantityUsed())
                        .append(" ").append(safeText(treatment.getUnit()))
                        .append(" | effectiveness=").append(treatment.getEffectiveness())
                        .append(" | result=").append(safeText(treatment.getResultSummary()))
                        .append("\n");
            }
        }
        sb.append("\n");

        sb.append("Field logs gan day (toi da ").append(MAX_FIELD_LOGS).append("):\n");
        if (fieldLogs.isEmpty()) {
            sb.append("- Khong co field log gan day.\n");
        } else {
            for (FieldLog fieldLog : fieldLogs) {
                sb.append("- ")
                        .append(fieldLog.getLogDate())
                        .append(" | type=").append(safeText(fieldLog.getLogType()))
                        .append(" | notes=").append(safeText(fieldLog.getNotes()))
                        .append("\n");
            }
        }
        sb.append("\n");

        sb.append("Incident context:\n");
        sb.append("- OpenIncidentCount: ").append(openIncidentCount).append("\n");
        if (linkedIncident != null) {
            sb.append("- LinkedIncident: id=").append(linkedIncident.getId())
                    .append(", type=").append(safeText(linkedIncident.getIncidentType()))
                    .append(", severity=").append(linkedIncident.getSeverity())
                    .append(", status=").append(linkedIncident.getStatus())
                    .append(", desc=").append(safeText(linkedIncident.getDescription()))
                    .append("\n");
        } else {
            sb.append("- LinkedIncident: none\n");
        }
        if (!incidents.isEmpty()) {
            sb.append("- RecentIncidents:\n");
            for (Incident incident : incidents) {
                sb.append("  - id=").append(incident.getId())
                        .append(", status=").append(incident.getStatus())
                        .append(", severity=").append(incident.getSeverity())
                        .append(", type=").append(safeText(incident.getIncidentType()))
                        .append(", desc=").append(safeText(incident.getDescription()))
                        .append("\n");
            }
        }
        sb.append("\n");

        sb.append("Inventory context:\n");
        if (!includeInventory) {
            sb.append("- Khong bao gom inventory theo yeu cau request.\n");
        } else if (inventoryRows.isEmpty()) {
            sb.append("- Chua co du lieu ton kho noi bo kha dung.\n");
        } else {
            sb.append("- Danh sach ton kho kha dung (toi da ").append(MAX_INVENTORY_ROWS).append(" dong):\n");
            for (InventoryBalance balance : inventoryRows) {
                SupplyLot lot = balance.getSupplyLot();
                String itemName = lot != null && lot.getSupplyItem() != null
                        ? safeText(lot.getSupplyItem().getName())
                        : "N/A";
                String activeIngredient = lot != null && lot.getSupplyItem() != null
                        ? safeText(lot.getSupplyItem().getActiveIngredient())
                        : "N/A";
                String unit = lot != null && lot.getSupplyItem() != null
                        ? safeText(lot.getSupplyItem().getUnit())
                        : "N/A";
                String batchCode = lot != null ? safeText(lot.getBatchCode()) : "N/A";
                String expiryDate = lot != null && lot.getExpiryDate() != null
                        ? lot.getExpiryDate().toString()
                        : "N/A";
                String lotStatus = lot != null ? safeText(lot.getStatus()) : "N/A";
                String warehouseName = balance.getWarehouse() != null
                        ? safeText(balance.getWarehouse().getName())
                        : "N/A";

                sb.append("  - item=").append(itemName)
                        .append(", active=").append(activeIngredient)
                        .append(", batch=").append(batchCode)
                        .append(", onHand=").append(nullSafeNumber(balance.getQuantity()))
                        .append(" ").append(unit)
                        .append(", expiry=").append(expiryDate)
                        .append(", lotStatus=").append(lotStatus)
                        .append(", warehouse=").append(warehouseName)
                        .append("\n");
            }
        }
        sb.append("\n");

        if (StringUtils.hasText(additionalNote)) {
            sb.append("Ghi chu bo sung tu nguoi dung:\n");
            sb.append("- ").append(safeText(additionalNote)).append("\n\n");
        }

        sb.append("Chi dung du lieu tren de goi y, khong suy dien thong tin ben ngoai.");
        return sb.toString();
    }

    private Map<String, Object> buildContextSummary(
            DiseaseRecord diseaseRecord,
            Season season,
            List<DiseaseTreatment> treatments,
            List<FieldLog> fieldLogs,
            List<Incident> incidents,
            boolean includeInventory,
            List<InventoryBalance> inventoryRows,
            String additionalNote,
            String question) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("seasonId", season.getId());
        summary.put("seasonName", season.getSeasonName());
        summary.put("diseaseRecordId", diseaseRecord.getId());
        summary.put("diseaseName", diseaseRecord.getDiseaseName());
        summary.put("severity", diseaseRecord.getSeverity() != null ? diseaseRecord.getSeverity().name() : null);
        summary.put("status", diseaseRecord.getStatus() != null ? diseaseRecord.getStatus().name() : null);
        summary.put("treatmentRows", treatments.size());
        summary.put("fieldLogRows", fieldLogs.size());
        summary.put("incidentRows", incidents.size());
        summary.put("includeInventory", includeInventory);
        summary.put("inventoryRows", inventoryRows.size());
        summary.put("hasAdditionalNote", StringUtils.hasText(additionalNote));
        summary.put("hasQuestion", StringUtils.hasText(question));
        return summary;
    }

    private void logSuggestionAudit(Integer diseaseRecordId, Map<String, Object> summary, LocalDateTime generatedAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>(summary);
        snapshot.put("generatedAt", generatedAt);
        snapshot.put("warning", WARNING_MESSAGE);

        auditLogService.logModuleOperation(
                "AI",
                "DISEASE_SUGGESTION",
                diseaseRecordId,
                "AI_DISEASE_SUGGESTION_REQUESTED",
                resolveAuditActor(),
                snapshot,
                null,
                null);
    }

    private String resolveTreatmentMaterialName(DiseaseTreatment treatment) {
        if (StringUtils.hasText(treatment.getMaterialName())) {
            return safeText(treatment.getMaterialName());
        }
        if (treatment.getSupplyItem() != null && StringUtils.hasText(treatment.getSupplyItem().getName())) {
            return safeText(treatment.getSupplyItem().getName());
        }
        return "N/A";
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeText(String value) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return "N/A";
        }
        if (normalized.length() <= MAX_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_TEXT_LENGTH) + "...";
    }

    private String nullSafeNumber(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "0";
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
}
