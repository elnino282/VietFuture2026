package org.example.season.service;

import lombok.RequiredArgsConstructor;
import org.example.season.dto.request.CreatePesticideRecordRequest;
import org.example.season.dto.response.PesticideRecordResponse;
import org.example.season.entity.PesticidePHIReference;
import org.example.season.entity.PesticideRecord;
import org.example.season.entity.Season;
import org.example.season.exception.AppException;
import org.example.season.exception.ErrorCode;
import org.example.season.repository.PesticidePHIReferenceRepository;
import org.example.season.repository.PesticideRecordRepository;
import org.example.season.repository.SeasonRepository;
import org.example.season.entity.FieldLog;
import org.example.season.repository.FieldLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PesticideRecordService {

    private final PesticideRecordRepository repository;
    private final PesticidePHIReferenceRepository pesticidePhiReferenceRepo;
    private final SeasonRepository seasonRepository;
    private final SeasonWorkspaceAccessService workspaceAccessService;
    private final FieldLogRepository fieldLogRepo;

    public PesticideRecordResponse create(Integer seasonId, CreatePesticideRecordRequest req, Long userId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));

        workspaceAccessService.assertCurrentUserCanAccessSeason(season);

        Integer phiDays = req.phiDays();
        String activeIngredient = req.activeIngredient();

        if (phiDays == null || phiDays <= 0) {
            // Lookup PHI từ reference table
            Optional<PesticidePHIReference> refOpt = pesticidePhiReferenceRepo
                    .findByPesticideNameContainingIgnoreCase(req.pesticideName());

            if (refOpt.isEmpty() && req.activeIngredient() != null) {
                refOpt = pesticidePhiReferenceRepo
                        .findByActiveIngredientContainingIgnoreCase(req.activeIngredient());
            }

            if (refOpt.isEmpty()) {
                refOpt = pesticidePhiReferenceRepo.findByName(req.pesticideName());
            }

            if (refOpt.isPresent()) {
                phiDays = refOpt.get().getPhiDays();
                if (activeIngredient == null || activeIngredient.isBlank()) {
                    activeIngredient = refOpt.get().getActiveIngredient();
                }
            } else {
                throw new IllegalArgumentException(
                        "Không tìm thấy thông tin PHI cho thuốc: " + req.pesticideName() +
                        ". Vui lòng nhập số ngày cách ly thủ công.");
            }
        }

        if (phiDays == null || phiDays < 0) {
            phiDays = 0;
        }

        PesticideRecord record = PesticideRecord.builder()
                .seasonId(seasonId)
                .plotId(season.getPlotId() != null ? season.getPlotId() : 0)
                .pesticideName(req.pesticideName())
                .activeIngredient(activeIngredient)
                .phiDays(phiDays)
                .applicationDate(req.applicationDate())
                .applicationMethod(req.applicationMethod())
                .dosage(req.dosage())
                .targetPest(req.targetPest())
                .note(req.note())
                .createdBy(userId)
                .build();

        PesticideRecord saved = repository.save(record);
        repository.flush();
        saved.setHarvestAllowedDate(saved.getApplicationDate().plusDays(saved.getPhiDays()));

        return toResponse(saved);
    }

    public PesticideRecordResponse createFromFieldLog(Integer fieldLogId, Long userId) {
        FieldLog log = fieldLogRepo.findById(fieldLogId)
                .orElseThrow(() -> new AppException(ErrorCode.FIELD_LOG_NOT_FOUND));

        // Parse thuốc từ notes hoặc dedicated field
        final String pesticideName = extractPesticideName(log.getNotes());

        // Lookup PHI từ reference table
        Integer phiDays = 0;
        if (pesticideName != null && !pesticideName.isBlank()) {
            phiDays = pesticidePhiReferenceRepo
                    .findByPesticideNameContainingIgnoreCase(pesticideName)
                    .map(PesticidePHIReference::getPhiDays)
                    .orElseGet(() -> {
                        return pesticidePhiReferenceRepo.findByActiveIngredientContainingIgnoreCase(pesticideName)
                                .map(PesticidePHIReference::getPhiDays)
                                .orElse(0); // Default to 0 if not found in reference table
                    });
        }

        final String recordPesticideName = (pesticideName != null && !pesticideName.isBlank())
                ? pesticideName
                : "Unknown/General Pesticide";

        // Xóa bản ghi cũ nếu có để tránh trùng lặp khi update field log
        repository.findByFieldLogId(fieldLogId).ifPresent(repository::delete);

        PesticideRecord record = PesticideRecord.builder()
                .seasonId(log.getSeason().getId())
                .plotId(log.getSeason().getPlotId() != null ? log.getSeason().getPlotId() : 0)
                .fieldLogId(fieldLogId)
                .pesticideName(recordPesticideName)
                .activeIngredient(recordPesticideName)
                .applicationDate(log.getLogDate())
                .phiDays(phiDays)
                .createdBy(userId)
                .build();

        PesticideRecord saved = repository.save(record);
        repository.flush();
        saved.setHarvestAllowedDate(saved.getApplicationDate().plusDays(saved.getPhiDays()));

        return toResponse(saved);
    }

    public void deleteByFieldLogId(Integer fieldLogId) {
        repository.findByFieldLogId(fieldLogId).ifPresent(repository::delete);
    }

    private String extractPesticideName(String notes) {
        if (notes == null || notes.isBlank()) {
            return "";
        }
        // Thử tìm trong reference table trước
        List<PesticidePHIReference> refs = pesticidePhiReferenceRepo.findAll();
        for (PesticidePHIReference ref : refs) {
            if (notes.toLowerCase().contains(ref.getActiveIngredient().toLowerCase())) {
                return ref.getActiveIngredient();
            }
            if (ref.getPesticideName() != null) {
                String[] names = ref.getPesticideName().split(",");
                for (String name : names) {
                    if (notes.toLowerCase().contains(name.trim().toLowerCase())) {
                        return name.trim();
                    }
                }
            }
        }
        // Fallback: Lấy nguyên note hoặc từ đầu tiên nếu note quá dài
        String cleanNote = notes.trim();
        if (cleanNote.contains(" ")) {
            return cleanNote.split(" ")[0];
        }
        return cleanNote;
    }

    @Transactional(readOnly = true)
    public List<PesticideRecordResponse> getActivePHI(Integer seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        workspaceAccessService.assertCurrentUserCanAccessSeason(season);

        LocalDate today = LocalDate.now();
        List<PesticideRecord> activeRecords = repository.findActivePHIBySeason(seasonId, today);
        return activeRecords.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PesticideRecordResponse> getBySeason(Integer seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));
        workspaceAccessService.assertCurrentUserCanAccessSeason(season);

        return repository.findBySeasonId(seasonId).stream().map(this::toResponse).toList();
    }

    private PesticideRecordResponse toResponse(PesticideRecord r) {
        LocalDate harvestAllowed = r.getHarvestAllowedDate();
        if (harvestAllowed == null && r.getApplicationDate() != null && r.getPhiDays() != null) {
            harvestAllowed = r.getApplicationDate().plusDays(r.getPhiDays());
        }
        return PesticideRecordResponse.builder()
                .id(r.getId())
                .seasonId(r.getSeasonId())
                .plotId(r.getPlotId())
                .fieldLogId(r.getFieldLogId())
                .pesticideName(r.getPesticideName())
                .activeIngredient(r.getActiveIngredient())
                .phiDays(r.getPhiDays())
                .harvestAllowedDate(harvestAllowed)
                .applicationDate(r.getApplicationDate())
                .applicationMethod(r.getApplicationMethod())
                .dosage(r.getDosage())
                .targetPest(r.getTargetPest())
                .note(r.getNote())
                .createdBy(r.getCreatedBy())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
