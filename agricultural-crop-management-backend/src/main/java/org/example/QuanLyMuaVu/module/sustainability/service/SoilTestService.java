package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateSoilTestRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SoilTestResponse;
import org.example.QuanLyMuaVu.module.sustainability.entity.SoilTest;
import org.example.QuanLyMuaVu.module.sustainability.repository.SoilTestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SoilTestService {

    static final String MINERAL_N_UNIT = "kg_n_per_ha";
    static final String CONCENTRATION_UNIT = "mg_per_kg";
    static final String CONTRIBUTION_UNIT = "kg_n";

    SoilTestRepository soilTestRepository;
    FarmerOwnershipService ownershipService;
    CurrentUserService currentUserService;

    public SoilTestResponse create(Integer seasonId, CreateSoilTestRequest request) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = ownershipService.requireOwnedSeason(seasonId);
        ensureSeasonOpenForSustainabilityWrite(season);
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = ownershipService.requireOwnedPlot(request.getPlotId());
        validatePlotBelongsToSeason(season, plot);

        NutrientInputSourceType sourceType = request.getSourceType();
        boolean measured = sourceType != null && sourceType.isMeasured();

        SoilTest saved = soilTestRepository.save(
                SoilTest.builder()
                        .seasonId(season.getId())
                        .season(season)
                        .plotId(plot.getId())
                        .plot(plot)
                        .sampleDate(request.getSampleDate())
                        .soilOrganicMatterPct(scale4(request.getSoilOrganicMatterPct()))
                        .mineralNKgPerHa(scale4(request.getMineralNKgPerHa()))
                        .nitrateMgPerKg(scale4(request.getNitrateMgPerKg()))
                        .ammoniumMgPerKg(scale4(request.getAmmoniumMgPerKg()))
                        .measured(measured)
                        .sourceType(sourceType)
                        .sourceDocument(trimToNull(request.getSourceDocument()))
                        .labReference(trimToNull(request.getLabReference()))
                        .note(trimToNull(request.getNote()))
                        .legacyDerived(Boolean.FALSE)
                        .legacyEventId(null)
                        .legacyNContributionKg(null)
                        .createdByUserId(currentUserService.getCurrentUserId())
                        .build()
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SoilTestResponse> list(Integer seasonId, Integer plotId) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = ownershipService.requireOwnedSeason(seasonId);

        List<SoilTest> items;
        if (plotId != null) {
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot = ownershipService.requireOwnedPlot(plotId);
            validatePlotBelongsToSeason(season, plot);
            items = soilTestRepository.findAllBySeasonIdAndPlotIdOrderBySampleDateDescCreatedAtDesc(seasonId, plotId);
        } else {
            items = soilTestRepository.findAllBySeasonIdOrderBySampleDateDescCreatedAtDesc(seasonId);
        }

        return items.stream()
                .sorted(Comparator
                        .comparing(SoilTest::getSampleDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SoilTest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SoilTest::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    private void validatePlotBelongsToSeason(org.example.QuanLyMuaVu.module.season.entity.Season season, org.example.QuanLyMuaVu.module.farm.entity.Plot plot) {
        if (season == null
                || season.getPlot() == null
                || plot == null
                || !Objects.equals(season.getPlot().getId(), plot.getId())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private void ensureSeasonOpenForSustainabilityWrite(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season == null || season.getStatus() == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        if (season.getStatus() == SeasonStatus.COMPLETED
                || season.getStatus() == SeasonStatus.CANCELLED
                || season.getStatus() == SeasonStatus.ARCHIVED) {
            throw new AppException(ErrorCode.INVALID_SEASON_STATUS_TRANSITION);
        }
    }

    private SoilTestResponse toResponse(SoilTest item) {
        NutrientInputSourceType sourceType = item.getSourceType();
        boolean measured = Boolean.TRUE.equals(item.getMeasured())
                || (sourceType != null && sourceType.isMeasured());

        return SoilTestResponse.builder()
                .id(item.getId())
                .seasonId(item.getSeasonId() != null
                        ? item.getSeasonId()
                        : item.getSeason() != null ? item.getSeason().getId() : null)
                .plotId(item.getPlotId() != null
                        ? item.getPlotId()
                        : item.getPlot() != null ? item.getPlot().getId() : null)
                .plotName(item.getPlot() != null ? item.getPlot().getPlotName() : null)
                .sampleDate(item.getSampleDate())
                .soilOrganicMatterPct(scale4(item.getSoilOrganicMatterPct()))
                .mineralNKgPerHa(scale4(item.getMineralNKgPerHa()))
                .nitrateMgPerKg(scale4(item.getNitrateMgPerKg()))
                .ammoniumMgPerKg(scale4(item.getAmmoniumMgPerKg()))
                .mineralNUnit(MINERAL_N_UNIT)
                .concentrationUnit(CONCENTRATION_UNIT)
                .estimatedNContributionKg(scale4(item.getLegacyNContributionKg() != null
                        ? item.getLegacyNContributionKg()
                        : calculateContribution(item.getMineralNKgPerHa(), item.getPlot())))
                .contributionUnit(CONTRIBUTION_UNIT)
                .measured(measured)
                .status(measured ? "measured" : "estimated")
                .sourceType(sourceType)
                .sourceDocument(item.getSourceDocument())
                .labReference(item.getLabReference())
                .note(item.getNote())
                .legacyDerived(Boolean.TRUE.equals(item.getLegacyDerived()))
                .migratedFromLegacyEventId(item.getLegacyEventId())
                .createdByUserId(item.getCreatedByUserId())
                .createdAt(item.getCreatedAt())
                .build();
    }

    private BigDecimal calculateContribution(BigDecimal mineralNKgPerHa, org.example.QuanLyMuaVu.module.farm.entity.Plot plot) {
        if (mineralNKgPerHa == null || plot == null || plot.getArea() == null || plot.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return mineralNKgPerHa.multiply(plot.getArea());
    }

    private BigDecimal scale4(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
