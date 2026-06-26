package org.example.sustainability.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.example.sustainability.snapshot.entity.FarmSnapshot;
import org.example.sustainability.snapshot.entity.SeasonSnapshot;
import org.example.sustainability.snapshot.model.FarmContext;
import org.example.sustainability.snapshot.model.SeasonContext;
import org.example.sustainability.snapshot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SnapshotQueryServiceTest {

    @Mock private FarmSnapshotRepository farmSnapshotRepository;
    @Mock private PlotSnapshotRepository plotSnapshotRepository;
    @Mock private SeasonSnapshotRepository seasonSnapshotRepository;
    @Mock private CropSnapshotRepository cropSnapshotRepository;
    @Mock private HarvestSnapshotRepository harvestSnapshotRepository;
    @Mock private ExpenseSnapshotRepository expenseSnapshotRepository;

    private SnapshotQueryService snapshotQueryService;

    @BeforeEach
    void setUp() {
        snapshotQueryService = new SnapshotQueryService(
                farmSnapshotRepository,
                plotSnapshotRepository,
                seasonSnapshotRepository,
                cropSnapshotRepository,
                harvestSnapshotRepository,
                expenseSnapshotRepository
        );
    }

    @Test
    void findFarm_ReadsLatestFarmSnapshot() {
        FarmSnapshot snapshot = FarmSnapshot.builder()
                .farmId(1)
                .userId(100L)
                .farmName("Updated Farm")
                .snapshotAt(LocalDateTime.now())
                .build();
        when(farmSnapshotRepository.findLatestByFarmId(1)).thenReturn(snapshot);

        FarmContext farm = snapshotQueryService.findFarm(1).orElseThrow();

        assertEquals("Updated Farm", farm.name());
        assertEquals(1, farm.id());
    }

    @Test
    void findSeason_ReadsLatestSeasonSnapshot() {
        SeasonSnapshot seasonSnapshot = SeasonSnapshot.builder()
                .seasonId(10)
                .seasonName("Season A")
                .plotId(2)
                .farmId(1)
                .cropId(3)
                .snapshotAt(LocalDateTime.now())
                .build();
        when(seasonSnapshotRepository.findLatestBySeasonId(10)).thenReturn(seasonSnapshot);
        when(cropSnapshotRepository.findLatestByCropId(3)).thenReturn(null);

        SeasonContext season = snapshotQueryService.findSeason(10).orElseThrow();

        assertEquals("Season A", season.seasonName());
        assertEquals(1, season.farmId());
    }
}
