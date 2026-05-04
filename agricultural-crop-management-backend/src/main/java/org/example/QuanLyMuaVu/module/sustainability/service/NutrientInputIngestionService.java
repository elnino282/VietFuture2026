package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.example.QuanLyMuaVu.Enums.NutrientInputSourceType;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.sustainability.dto.request.CreateNutrientInputRequest;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.NutrientInputResponse;
import org.example.QuanLyMuaVu.module.sustainability.entity.NutrientInputEvent;
import org.example.QuanLyMuaVu.module.sustainability.repository.NutrientInputEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class NutrientInputIngestionService {

    static final String UNIT_KG_N = "kg_n";
    static final String UNIT_KG_N_PER_HA = "kg_n_per_ha";
    static final Set<NutrientInputSource> DEPRECATED_LEGACY_SOURCES = EnumSet.of(
            NutrientInputSource.IRRIGATION_WATER,
            NutrientInputSource.SOIL_LEGACY
    );

    NutrientInputEventRepository nutrientInputEventRepository;
    FarmerOwnershipService ownershipService;
    CurrentUserService currentUserService;

    public NutrientInputResponse create(Integer seasonId, CreateNutrientInputRequest request) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = ownershipService.requireOwnedSeason(seasonId);
        ensureSeasonOpenForSustainabilityWrite(season);
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = ownershipService.requireOwnedPlot(request.getPlotId());
        validatePlotBelongsToSeason(season, plot);
        validateLegacySourceDeprecation(request.getInputSource());

        String normalizedUnit = normalizeUnit(request.getUnit());
        BigDecimal normalizedNKg = normalizeToKgN(request.getValue(), normalizedUnit, plot);
        NutrientInputSourceType sourceType = request.getSourceType();
        boolean measured = sourceType != null && sourceType.isMeasured();

        NutrientInputEvent saved = nutrientInputEventRepository.save(
                NutrientInputEvent.builder()
                        .seasonId(season.getId())
                        .season(season)
                        .plotId(plot.getId())
                        .plot(plot)
                        .inputSource(request.getInputSource())
                        .nKg(scale4(normalizedNKg))
                        .appliedDate(request.getRecordedAt())
                        .measured(measured)
                        .dataSource(sourceType != null ? sourceType.getApiValue() : null)
                        .sourceType(sourceType)
                        .sourceDocument(trimToNull(request.getSourceDocument()))
                        .note(trimToNull(request.getNote()))
                        .createdByUserId(currentUserService.getCurrentUserId())
                        .build()
        );

        return toResponse(saved, UNIT_KG_N);
    }

    @Transactional(readOnly = true)
    public List<NutrientInputResponse> list(Integer seasonId, Integer plotId, NutrientInputSource source) {
        org.example.QuanLyMuaVu.module.season.entity.Season season = ownershipService.requireOwnedSeason(seasonId);

        List<NutrientInputEvent> events;
        if (plotId != null) {
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot = ownershipService.requireOwnedPlot(plotId);
            validatePlotBelongsToSeason(season, plot);
            events = nutrientInputEventRepository.findAllBySeasonIdAndPlotId(seasonId, plotId);
        } else {
            events = nutrientInputEventRepository.findAllBySeasonId(seasonId);
        }

        return events.stream()
                .filter(item -> source == null || source == item.getInputSource())
                .sorted(Comparator
                        .comparing(NutrientInputEvent::getAppliedDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(NutrientInputEvent::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(NutrientInputEvent::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> toResponse(item, UNIT_KG_N))
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

    private void validateLegacySourceDeprecation(NutrientInputSource source) {
        if (source != null && DEPRECATED_LEGACY_SOURCES.contains(source)) {
            throw new AppException(ErrorCode.LEGACY_NUTRIENT_INPUT_DEPRECATED);
        }
    }

    private String normalizeUnit(String unit) {
        if (!StringUtils.hasText(unit)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        String normalized = unit.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "_");
        if (UNIT_KG_N.equals(normalized) || "kgn".equals(normalized) || "kgn_per_season".equals(normalized)) {
            return UNIT_KG_N;
        }
        if (UNIT_KG_N_PER_HA.equals(normalized)
                || "kg_n/ha".equals(normalized)
                || "kgn/ha".equals(normalized)
                || "kg_n_ha".equals(normalized)) {
            return UNIT_KG_N_PER_HA;
        }
        throw new AppException(ErrorCode.BAD_REQUEST);
    }

    private BigDecimal normalizeToKgN(BigDecimal value, String unit, org.example.QuanLyMuaVu.module.farm.entity.Plot plot) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (UNIT_KG_N.equals(unit)) {
            return value;
        }
        BigDecimal area = plot != null ? plot.getArea() : null;
        if (area == null || area.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return value.multiply(area);
    }

    private NutrientInputResponse toResponse(NutrientInputEvent event, String unit) {
        NutrientInputSourceType sourceType = event.getSourceType();
        if (sourceType == null && StringUtils.hasText(event.getDataSource())) {
            try {
                sourceType = NutrientInputSourceType.fromApiValue(event.getDataSource());
            } catch (IllegalArgumentException ignored) {
                sourceType = null;
            }
        }
        String status = Boolean.TRUE.equals(event.getMeasured()) ? "measured" : "estimated";
        return NutrientInputResponse.builder()
                .id(event.getId())
                .seasonId(event.getSeasonId() != null
                        ? event.getSeasonId()
                        : event.getSeason() != null ? event.getSeason().getId() : null)
                .plotId(event.getPlotId() != null
                        ? event.getPlotId()
                        : event.getPlot() != null ? event.getPlot().getId() : null)
                .plotName(event.getPlot() != null ? event.getPlot().getPlotName() : null)
                .inputSource(event.getInputSource())
                .value(scale4(event.getNKg()))
                .unit(unit)
                .normalizedNKg(scale4(event.getNKg()))
                .recordedAt(event.getAppliedDate())
                .measured(event.getMeasured())
                .status(status)
                .sourceType(sourceType)
                .sourceDocument(event.getSourceDocument())
                .note(event.getNote())
                .createdByUserId(event.getCreatedByUserId())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal scale4(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }
}
