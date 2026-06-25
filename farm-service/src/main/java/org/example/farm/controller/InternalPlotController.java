package org.example.farm.controller;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.farm.entity.Plot;
import org.example.farm.repository.PlotRepository;
import org.example.farm.repository.FarmRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalPlotController {

    private final PlotRepository plotRepository;
    private final FarmRepository farmRepository;

    @GetMapping("/users/{userId}/farms/ids")
    public ResponseEntity<java.util.List<Integer>> getFarmIdsByUserId(@PathVariable Long userId) {
        java.util.List<Integer> farmIds = farmRepository.findAllByUserId(userId).stream()
                .map(org.example.farm.entity.Farm::getId)
                .toList();
        return ResponseEntity.ok(farmIds);
    }

    @GetMapping("/plots/{id}")
    public ResponseEntity<PlotInternalDto> getPlotInternal(@PathVariable Integer id) {
        Plot plot = plotRepository.findById(id).orElse(null);

        if (plot == null) {
            return ResponseEntity.notFound().build();
        }
        
        PlotInternalDto dto = PlotInternalDto.builder()
                .id(plot.getId())
                .plotName(plot.getPlotName())
                .farmId(plot.getFarm() != null ? plot.getFarm().getId() : null)
                .farmName(plot.getFarm() != null ? plot.getFarm().getName() : null)
                .ownerUserId(plot.getFarm() != null ? plot.getFarm().getUserId() : null)
                .farmActive(plot.getFarm() != null ? plot.getFarm().getActive() : null)
                .build();
                
        return ResponseEntity.ok(dto);
    }

    @Data
    @Builder
    public static class PlotInternalDto {
        private Integer id;
        private String plotName;
        private Integer farmId;
        private String farmName;
        private Long ownerUserId;
        private Boolean farmActive;
    }
}
