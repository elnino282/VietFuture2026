package org.example.farm.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.farm.entity.Farm;
import org.example.farm.repository.FarmRepository;
import org.example.farm.mapper.FarmMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/farms")
@RequiredArgsConstructor
public class InternalFarmController {

    private final FarmRepository farmRepository;
    private final FarmMapper farmMapper;

    @PostMapping("/batch")
    public ResponseEntity<List<FarmSummaryDto>> getFarmsByIds(@RequestBody List<Integer> farmIds) {
        List<Farm> farms = farmRepository.findAllById(farmIds);
        List<FarmSummaryDto> dtos = farms.stream()
                .map(farmMapper::toSummaryDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{farmId}")
    public ResponseEntity<FarmDetailDto> getFarmDetail(@PathVariable Integer farmId) {
        return farmRepository.findById(farmId)
                .map(farmMapper::toDetailDto)
                .map(ResponseEntity::ok)
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
