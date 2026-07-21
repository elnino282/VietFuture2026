package org.example.sustainability.controller;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.sustainability.entity.SoilTest;
import org.example.sustainability.entity.IrrigationWaterAnalysis;
import org.example.sustainability.entity.NutrientInputEvent;
import org.example.sustainability.repository.SoilTestRepository;
import org.example.sustainability.repository.IrrigationWaterAnalysisRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalSustainabilityController {

    private final SoilTestRepository soilTestRepository;
    private final IrrigationWaterAnalysisRepository irrigationWaterAnalysisRepository;
    private final org.example.sustainability.repository.NutrientInputEventRepository nutrientInputEventRepository;

    @GetMapping("/seasons/{seasonId}/soil-tests")
    public ResponseEntity<List<SoilTestInternalDto>> getSoilTestsInternal(@PathVariable Integer seasonId) {
        List<SoilTest> items = soilTestRepository.findAllBySeasonIdOrderBySampleDateDescCreatedAtDesc(seasonId);
        List<SoilTestInternalDto> dtos = items.stream()
                .map(item -> SoilTestInternalDto.builder()
                        .id(item.getId())
                        .seasonId(item.getSeasonId())
                        .plotId(item.getPlotId())
                        .sampleDate(item.getSampleDate())
                        .measured(item.getMeasured())
                        .build())
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/seasons/{seasonId}/irrigation-water-analyses")
    public ResponseEntity<List<IrrigationWaterAnalysisInternalDto>> getWaterAnalysesInternal(@PathVariable Integer seasonId) {
        List<IrrigationWaterAnalysis> items = irrigationWaterAnalysisRepository.findAllBySeasonIdOrderBySampleDateDescCreatedAtDesc(seasonId);
        List<IrrigationWaterAnalysisInternalDto> dtos = items.stream()
                .map(item -> IrrigationWaterAnalysisInternalDto.builder()
                        .id(item.getId())
                        .seasonId(item.getSeasonId())
                        .plotId(item.getPlotId())
                        .sampleDate(item.getSampleDate())
                        .measured(item.getMeasured())
                        .build())
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/seasons/{seasonId}/nutrient-inputs")
    public ResponseEntity<List<NutrientInputEventInternalDto>> getNutrientInputsInternal(@PathVariable Integer seasonId) {
        List<NutrientInputEvent> items = nutrientInputEventRepository.findAllBySeasonId(seasonId);

        List<NutrientInputEventInternalDto> dtos = items.stream()
                .map(item -> NutrientInputEventInternalDto.builder()
                        .id(item.getId())
                        .seasonId(item.getSeasonId())
                        .appliedDate(item.getAppliedDate())
                        .inputSource(item.getInputSource().name())
                        .nKg(item.getNKg())
                        .build())
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoilTestInternalDto {
        private Integer id;
        private Integer seasonId;
        private Integer plotId;
        private java.time.LocalDate sampleDate;
        private Boolean measured;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IrrigationWaterAnalysisInternalDto {
        private Integer id;
        private Integer seasonId;
        private Integer plotId;
        private java.time.LocalDate sampleDate;
        private Boolean measured;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutrientInputEventInternalDto {
        private Integer id;
        private Integer seasonId;
        private java.time.LocalDate appliedDate;
        private String inputSource;
        private java.math.BigDecimal nKg;
    }
}
