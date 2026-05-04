package org.example.QuanLyMuaVu.module.farm.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.farm.dto.request.PlotRequest;
import org.example.QuanLyMuaVu.module.farm.dto.response.PlotResponse;
import org.example.QuanLyMuaVu.module.farm.service.PlotService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('FARMER')")
public class PlotController {

        private final PlotService plotService;

        // --- Plot Management (by Farm) ---

        @GetMapping("/farms/{farmId}/plots")
        public ResponseEntity<ApiResponse<List<PlotResponse>>> listPlotsByFarm(@PathVariable Integer farmId) {
                return ResponseEntity.ok(ApiResponse.success(plotService.listPlotsByFarm(farmId)));
        }

        @PostMapping("/farms/{farmId}/plots")
        public ResponseEntity<ApiResponse<PlotResponse>> createPlot(
                        @PathVariable Integer farmId,
                        @Valid @RequestBody PlotRequest request) {
                return ResponseEntity.ok(ApiResponse.success(plotService.createPlotForFarm(farmId, request)));
        }

        // --- Plot Management (Direct) ---

        @GetMapping("/plots")
        public ResponseEntity<ApiResponse<List<PlotResponse>>> listAllPlots() {
                return ResponseEntity.ok(ApiResponse.success(plotService.listPlotsForCurrentFarmer()));
        }

        @GetMapping("/plots/{id}")
        public ResponseEntity<ApiResponse<PlotResponse>> getPlot(@PathVariable Integer id) {
                return ResponseEntity.ok(ApiResponse.success(plotService.getPlotForCurrentFarmer(id)));
        }

        @PutMapping("/plots/{id}")
        public ResponseEntity<ApiResponse<PlotResponse>> updatePlot(
                        @PathVariable Integer id,
                        @Valid @RequestBody PlotRequest request) {
                return ResponseEntity.ok(ApiResponse.success(plotService.updatePlotForCurrentFarmer(id, request)));
        }

        @DeleteMapping("/plots/{id}")
        public ResponseEntity<ApiResponse<Void>> deletePlot(@PathVariable Integer id) {
                plotService.deletePlotForCurrentFarmer(id);
                return ResponseEntity.ok(ApiResponse.success(null));
        }
}
