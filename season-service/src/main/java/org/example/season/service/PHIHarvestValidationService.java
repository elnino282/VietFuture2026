package org.example.season.service;

import lombok.RequiredArgsConstructor;
import org.example.season.entity.PesticideRecord;
import org.example.season.repository.PesticideRecordRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PHIHarvestValidationService {

    private final PesticideRecordRepository pesticideRepo;

    /**
     * Validate xem có thể thu hoạch được không.
     * @return HarvestValidationResult — isBlocked, violations list
     */
    public HarvestValidationResult validateHarvest(Integer seasonId, LocalDate harvestDate) {
        List<PesticideRecord> violations =
            pesticideRepo.findPHIViolations(seasonId, harvestDate);

        if (violations.isEmpty()) {
            return new HarvestValidationResult(false, List.of(), null);
        }

        // Tính ngày an toàn gần nhất
        LocalDate nearestSafeDate = violations.stream()
            .map(PesticideRecord::getHarvestAllowedDate)
            .max(LocalDate::compareTo)
            .orElse(harvestDate);

        List<PHIViolationDetail> details = violations.stream()
            .map(v -> new PHIViolationDetail(
                v.getPesticideName(),
                v.getActiveIngredient(),
                v.getApplicationDate(),
                v.getHarvestAllowedDate(),
                v.getPhiDays(),
                v.getHarvestAllowedDate().toEpochDay() - harvestDate.toEpochDay() // số ngày còn cách ly
            ))
            .toList();

        return new HarvestValidationResult(true, details, nearestSafeDate);
    }

    public record HarvestValidationResult(
        boolean isBlocked,
        List<PHIViolationDetail> violations,
        LocalDate nearestSafeDate
    ) {}

    public record PHIViolationDetail(
        String pesticideName,
        String activeIngredient,
        LocalDate applicationDate,
        LocalDate harvestAllowedDate,
        int phiDays,
        long daysRemaining  // số ngày còn phải chờ
    ) {}
}
