package org.example.QuanLyMuaVu.module.sustainability.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.FieldMapResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
@Slf4j
public class SustainabilityDashboardContextService {

    CurrentUserService currentUserService;
    FarmerOwnershipService ownershipService;
    FarmQueryPort farmQueryPort;
    SeasonQueryPort seasonQueryPort;
    SustainabilityCalculationService calculationService;
    SustainabilityDashboardMetricSupport metricSupport;
    ObjectMapper objectMapper;

    FieldContext resolveFieldContext(Integer seasonId, Integer fieldId, Integer farmId) {
        if (seasonId != null) {
            org.example.QuanLyMuaVu.module.season.entity.Season season = ownershipService.requireOwnedSeason(seasonId);
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot = season.getPlot();
            if (fieldId != null && !Objects.equals(plot.getId(), fieldId)) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            if (farmId != null && (plot.getFarm() == null || !Objects.equals(plot.getFarm().getId(), farmId))) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            return new FieldContext(plot, season);
        }

        if (fieldId != null) {
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot = ownershipService.requireOwnedPlot(fieldId);
            if (farmId != null && (plot.getFarm() == null || !Objects.equals(plot.getFarm().getId(), farmId))) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            return new FieldContext(plot, resolvePreferredSeasonForPlot(plot.getId()));
        }

        Long ownerId = currentUserService.getCurrentUserId();
        List<org.example.QuanLyMuaVu.module.season.entity.Season> active = seasonQueryPort.findActiveSeasonsByOwnerIdOrderByStartDateDesc(ownerId);
        if (!active.isEmpty()) {
            return new FieldContext(active.get(0).getPlot(), active.get(0));
        }
        List<org.example.QuanLyMuaVu.module.farm.entity.Plot> plots = farmId != null
                ? farmQueryPort.findPlotsByOwnerIdAndFarmId(ownerId, farmId)
                : farmQueryPort.findPlotsByOwnerId(ownerId);
        if (plots.isEmpty()) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }
        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = plots.get(0);
        return new FieldContext(plot, resolvePreferredSeasonForPlot(plot.getId()));
    }

    org.example.QuanLyMuaVu.module.farm.entity.Farm resolveFarmContext(Integer seasonId, Integer fieldId, Integer farmId) {
        if (seasonId != null) {
            org.example.QuanLyMuaVu.module.season.entity.Season season = ownershipService.requireOwnedSeason(seasonId);
            org.example.QuanLyMuaVu.module.farm.entity.Farm farm = season.getPlot() != null ? season.getPlot().getFarm() : null;
            if (farmId != null && (farm == null || !Objects.equals(farm.getId(), farmId))) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            return farm;
        }
        if (fieldId != null) {
            org.example.QuanLyMuaVu.module.farm.entity.Plot plot = ownershipService.requireOwnedPlot(fieldId);
            if (farmId != null && (plot.getFarm() == null || !Objects.equals(plot.getFarm().getId(), farmId))) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            return plot.getFarm();
        }
        if (farmId != null) {
            return ownershipService.requireOwnedFarm(farmId);
        }
        List<org.example.QuanLyMuaVu.module.farm.entity.Farm> farms = ownershipService.getOwnedFarms();
        return farms.isEmpty() ? null : farms.get(0);
    }

    org.example.QuanLyMuaVu.module.season.entity.Season resolvePreferredSeasonForPlot(Integer plotId) {
        List<org.example.QuanLyMuaVu.module.season.entity.Season> seasons = seasonQueryPort.findAllSeasonsByPlotIdOrderByStartDateDesc(plotId);
        if (seasons.isEmpty()) {
            return null;
        }
        return seasons.stream()
                .filter(item -> item.getStatus() != null && "ACTIVE".equalsIgnoreCase(item.getStatus().name()))
                .findFirst()
                .orElse(seasons.get(0));
    }

    org.example.QuanLyMuaVu.module.season.entity.Season resolveSeasonForMap(org.example.QuanLyMuaVu.module.farm.entity.Plot plot, org.example.QuanLyMuaVu.module.season.entity.Season selectedSeason) {
        if (selectedSeason != null) {
            return Objects.equals(selectedSeason.getPlot().getId(), plot.getId()) ? selectedSeason : null;
        }
        return resolvePreferredSeasonForPlot(plot.getId());
    }

    List<SustainabilityOverviewResponse.HistoryPoint> buildHistoryForPlot(org.example.QuanLyMuaVu.module.farm.entity.Plot plot) {
        List<SustainabilityOverviewResponse.HistoryPoint> history = new ArrayList<>();
        for (org.example.QuanLyMuaVu.module.season.entity.Season season : seasonQueryPort.findAllSeasonsByPlotIdOrderByStartDateAsc(plot.getId())) {
            SustainabilityCalculationService.CalculationResult result = calculationService.calculate(season, plot);
            history.add(SustainabilityOverviewResponse.HistoryPoint.builder()
                    .seasonId(season.getId())
                    .seasonName(season.getSeasonName())
                    .startDate(season.getStartDate())
                    .fdnTotal(result.getFdnTotal())
                    .fdnMineral(result.getFdnMineral())
                    .fdnOrganic(result.getFdnOrganic())
                    .nue(result.getNue())
                    .nOutput(result.getNOutput())
                    .yield(result.getYieldValue())
                    .build());
        }
        return history;
    }

    SustainabilityOverviewResponse.CurrentSeason buildCurrentSeason(org.example.QuanLyMuaVu.module.season.entity.Season season) {
        if (season == null) {
            return null;
        }
        Integer dayCount = null;
        if (season.getStartDate() != null) {
            dayCount = (int) Math.max(1, ChronoUnit.DAYS.between(season.getStartDate(), LocalDate.now()) + 1);
        }
        return SustainabilityOverviewResponse.CurrentSeason.builder()
                .seasonName(season.getSeasonName())
                .cropName(season.getCrop() != null ? season.getCrop().getCropName() : "N/A")
                .dayCount(dayCount)
                .stage(season.getStatus() != null ? season.getStatus().name() : "UNKNOWN")
                .build();
    }

    JsonNode parseGeometry(String boundaryGeoJson) {
        if (!StringUtils.hasText(boundaryGeoJson)) {
            return null;
        }
        try {
            JsonNode geometry = objectMapper.readTree(boundaryGeoJson);
            return geometry.has("type") ? geometry : null;
        } catch (Exception ex) {
            log.warn("Failed to parse plot boundary geojson", ex);
            return null;
        }
    }

    FieldMapResponse.LatLng deriveCenter(JsonNode geometry) {
        if (geometry == null || !geometry.has("coordinates")) {
            return null;
        }
        double[] acc = new double[] { 0, 0, 0 };
        collectCoordinates(geometry.get("coordinates"), acc);
        if (acc[2] <= 0) {
            return null;
        }
        return FieldMapResponse.LatLng.builder()
                .lat(BigDecimal.valueOf(acc[1] / acc[2]).setScale(6, RoundingMode.HALF_UP))
                .lng(BigDecimal.valueOf(acc[0] / acc[2]).setScale(6, RoundingMode.HALF_UP))
                .build();
    }

    private void collectCoordinates(JsonNode node, double[] acc) {
        if (node == null) {
            return;
        }
        if (node.isArray() && node.size() >= 2 && node.get(0).isNumber() && node.get(1).isNumber()) {
            acc[0] += node.get(0).asDouble();
            acc[1] += node.get(1).asDouble();
            acc[2] += 1;
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectCoordinates(child, acc);
            }
        }
    }

    record FieldContext(org.example.QuanLyMuaVu.module.farm.entity.Plot plot, org.example.QuanLyMuaVu.module.season.entity.Season season) {
    }
}
