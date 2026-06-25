package org.example.QuanLyMuaVu.module.season.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/seasons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicSeasonController {

    SeasonQueryPort seasonQueryPort;

    @GetMapping("/exists-by-variety/{varietyId}")
    public boolean existsSeasonByVarietyId(@PathVariable Integer varietyId) {
        return seasonQueryPort.existsSeasonByVarietyId(varietyId);
    }

    @GetMapping("/exists-active-by-plot/{plotId}")
    public boolean existsActiveSeasonByPlotId(@PathVariable Integer plotId) {
        return seasonQueryPort.existsActiveSeasonByPlotId(plotId);
    }

    @GetMapping("/exists-active-tasks-by-plot/{plotId}")
    public boolean existsActiveTasksByPlotId(@PathVariable Integer plotId) {
        return seasonQueryPort.existsActiveTasksByPlotId(plotId);
    }
}
