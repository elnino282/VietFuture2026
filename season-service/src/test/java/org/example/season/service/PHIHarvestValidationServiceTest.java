package org.example.season.service;

import org.example.season.entity.PesticideRecord;
import org.example.season.repository.PesticideRecordRepository;
import org.example.season.service.PHIHarvestValidationService.HarvestValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PHIHarvestValidationServiceTest {

    @Mock
    private PesticideRecordRepository pesticideRepo;

    @InjectMocks
    private PHIHarvestValidationService phiService;

    private Integer seasonId;
    private LocalDate harvestDate;

    @BeforeEach
    void setUp() {
        seasonId = 1;
        harvestDate = LocalDate.of(2026, 7, 7);
    }

    @Test
    void validateHarvest_noViolations_returnsNotBlocked() {
        when(pesticideRepo.findPHIViolations(seasonId, harvestDate))
                .thenReturn(Collections.emptyList());

        HarvestValidationResult result = phiService.validateHarvest(seasonId, harvestDate);

        assertFalse(result.isBlocked());
        assertTrue(result.violations().isEmpty());
        assertNull(result.nearestSafeDate());
    }

    @Test
    void validateHarvest_withViolations_returnsBlockedWithDetails() {
        PesticideRecord record1 = PesticideRecord.builder()
                .pesticideName("Abamectin")
                .activeIngredient("Abamectin")
                .applicationDate(LocalDate.of(2026, 7, 1))
                .phiDays(14)
                .build();
        record1.setHarvestAllowedDate(record1.getApplicationDate().plusDays(record1.getPhiDays())); // 2026-07-15

        PesticideRecord record2 = PesticideRecord.builder()
                .pesticideName("Metalaxyl")
                .activeIngredient("Metalaxyl")
                .applicationDate(LocalDate.of(2026, 7, 5))
                .phiDays(7)
                .build();
        record2.setHarvestAllowedDate(record2.getApplicationDate().plusDays(record2.getPhiDays())); // 2026-07-12

        when(pesticideRepo.findPHIViolations(seasonId, harvestDate))
                .thenReturn(List.of(record1, record2));

        HarvestValidationResult result = phiService.validateHarvest(seasonId, harvestDate);

        assertTrue(result.isBlocked());
        assertEquals(2, result.violations().size());
        assertEquals(LocalDate.of(2026, 7, 15), result.nearestSafeDate()); // max of 2026-07-15 and 2026-07-12

        // Verify violation details
        var violationDetails = result.violations();
        assertEquals("Abamectin", violationDetails.get(0).pesticideName());
        assertEquals(14, violationDetails.get(0).phiDays());
        assertEquals(LocalDate.of(2026, 7, 15), violationDetails.get(0).harvestAllowedDate());
        assertEquals(8, violationDetails.get(0).daysRemaining()); // 2026-07-15 to 2026-07-07
    }
}
