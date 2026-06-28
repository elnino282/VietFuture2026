package org.example.farm.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.farm.entity.Farm;
import org.example.farm.repository.FarmRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/farms")
@RequiredArgsConstructor
public class InternalFarmController {

    private final FarmRepository farmRepository;

    @PostMapping("/batch")
    public ResponseEntity<List<FarmSummaryDto>> getFarmsByIds(@RequestBody List<Integer> farmIds) {
        List<Farm> farms = farmRepository.findAllById(farmIds);
        List<FarmSummaryDto> dtos = farms.stream()
                .map(farm -> FarmSummaryDto.builder()
                        .id(farm.getId())
                        .name(farm.getName())
                        .provinceName(farm.getProvince() != null ? farm.getProvince().getName() : null)
                        .wardName(farm.getWard() != null ? farm.getWard().getName() : null)
                        .area(farm.getArea())
                        .build())
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{farmId}")
    public ResponseEntity<FarmDetailDto> getFarmDetail(@PathVariable Integer farmId) {
        return farmRepository.findById(farmId)
                .map(farm -> ResponseEntity.ok(FarmDetailDto.builder()
                        .id(farm.getId())
                        .name(farm.getName())
                        .provinceName(farm.getProvince() != null ? farm.getProvince().getName() : null)
                        .wardName(farm.getWard() != null ? farm.getWard().getName() : null)
                        .area(farm.getArea())
                        .latitude(farm.getLatitude())
                        .longitude(farm.getLongitude())
                        .averageRating(farm.getAverageRating())
                        .userId(farm.getUserId())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FarmSummaryDto {
        private Integer id;
        private String name;
        private String provinceName;
        private String wardName;
        private java.math.BigDecimal area;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FarmDetailDto {
        private Integer id;
        private String name;
        private String provinceName;
        private String wardName;
        private java.math.BigDecimal area;
        private java.math.BigDecimal latitude;
        private java.math.BigDecimal longitude;
        private Double averageRating;
        private Long userId;
    }
}
