package org.example.QuanLyMuaVu.module.sustainability.service;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.incident.port.IncidentQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.PlotStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardPlotStatusReadService {

    private final CurrentUserService currentUserService;
    private final FarmQueryPort farmQueryPort;
    private final SeasonQueryPort seasonQueryPort;
    private final IncidentQueryPort incidentQueryPort;

    public List<PlotStatusResponse> getPlotStatus(Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        List<org.example.QuanLyMuaVu.module.farm.entity.Plot> plots = farmQueryPort.findPlotsByOwnerId(ownerId);
        return plots.stream().map(this::mapToPlotStatusResponse).toList();
    }

    private PlotStatusResponse mapToPlotStatusResponse(org.example.QuanLyMuaVu.module.farm.entity.Plot plot) {
        String cropName = "N/A";
        String stage = "N/A";
        String health = "HEALTHY";

        List<org.example.QuanLyMuaVu.module.season.entity.Season> seasons = seasonQueryPort.findAllSeasonsByPlotId(plot.getId());
        if (!seasons.isEmpty()) {
            org.example.QuanLyMuaVu.module.season.entity.Season latestSeason = seasons.stream()
                    .max(Comparator.comparing(org.example.QuanLyMuaVu.module.season.entity.Season::getStartDate))
                    .orElse(null);
            if (latestSeason != null) {
                if (latestSeason.getCrop() != null) {
                    cropName = latestSeason.getCrop().getCropName();
                }
                stage = latestSeason.getStatus() != null ? latestSeason.getStatus().name() : "N/A";

                long openCount = incidentQueryPort.countOpenIncidentsBySeasonId(latestSeason.getId());
                if (openCount > 2) {
                    health = "CRITICAL";
                } else if (openCount > 0) {
                    health = "WARNING";
                }
            }
        }

        return PlotStatusResponse.builder()
                .plotId(plot.getId())
                .plotName(plot.getPlotName())
                .areaHa(plot.getArea())
                .cropName(cropName)
                .stage(stage)
                .health(health)
                .build();
    }
}
