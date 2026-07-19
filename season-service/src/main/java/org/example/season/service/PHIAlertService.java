package org.example.season.service;

import lombok.RequiredArgsConstructor;
import org.example.season.dto.response.PHIAlertDto;
import org.example.season.entity.PesticideRecord;
import org.example.season.entity.Season;
import org.example.season.enums.SeasonStatus;
import org.example.season.repository.PesticideRecordRepository;
import org.example.season.repository.SeasonRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PHIAlertService {

    private final SeasonRepository seasonRepository;
    private final PesticideRecordRepository pesticideRecordRepository;

    /**
     * Lấy danh sách các cảnh báo PHI (PesticideRecord chưa qua thời gian cách ly)
     * cho tất cả các mùa vụ đang ACTIVE của nông dân hiện tại.
     */
    public List<PHIAlertDto> getActivePHIAlerts(Long userId) {
        // 1. Lấy danh sách các Season đang ACTIVE của userId
        List<Season> activeSeasons = seasonRepository.findActiveSeasonsByUserIdOrderByStartDateDesc(SeasonStatus.ACTIVE, userId);
        if (activeSeasons.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> seasonIds = activeSeasons.stream().map(Season::getId).collect(Collectors.toList());
        Map<Integer, String> seasonNameMap = activeSeasons.stream().collect(Collectors.toMap(Season::getId, Season::getSeasonName));

        // 2. Tìm các PesticideRecord có harvestAllowedDate > today
        LocalDate today = LocalDate.now();
        List<PesticideRecord> activeRecords = pesticideRecordRepository.findActivePHIBySeasonIds(seasonIds, today);

        // 3. Map sang DTO
        return activeRecords.stream().map(record -> {
            long daysRemaining = ChronoUnit.DAYS.between(today, record.getHarvestAllowedDate());
            return PHIAlertDto.builder()
                    .seasonId(record.getSeasonId())
                    .seasonName(seasonNameMap.get(record.getSeasonId()))
                    .pesticideName(record.getPesticideName())
                    .appliedDate(record.getApplicationDate())
                    .requiredIntervalDays(record.getPhiDays())
                    .earliestSafeDate(record.getHarvestAllowedDate())
                    .daysRemaining(daysRemaining)
                    .build();
        }).collect(Collectors.toList());
    }
}

