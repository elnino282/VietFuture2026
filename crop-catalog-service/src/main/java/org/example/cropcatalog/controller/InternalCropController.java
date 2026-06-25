package org.example.cropcatalog.controller;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.cropcatalog.entity.Crop;
import org.example.cropcatalog.entity.Variety;
import org.example.cropcatalog.repository.CropRepository;
import org.example.cropcatalog.repository.VarietyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalCropController {

    private final CropRepository cropRepository;
    private final VarietyRepository varietyRepository;

    @GetMapping("/crops/{id}")
    public ResponseEntity<CropInternalDto> getCropInternal(@PathVariable Integer id) {
        Crop crop = cropRepository.findById(id).orElse(null);
        if (crop == null) {
            return ResponseEntity.notFound().build();
        }
        CropInternalDto dto = CropInternalDto.builder()
                .id(crop.getId())
                .cropName(crop.getCropName())
                .build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/varieties/{id}")
    public ResponseEntity<VarietyInternalDto> getVarietyInternal(@PathVariable Integer id) {
        Variety variety = varietyRepository.findById(id).orElse(null);
        if (variety == null) {
            return ResponseEntity.notFound().build();
        }
        VarietyInternalDto dto = VarietyInternalDto.builder()
                .id(variety.getId())
                .name(variety.getName())
                .cropId(variety.getCrop() != null ? variety.getCrop().getId() : null)
                .build();
        return ResponseEntity.ok(dto);
    }

    @Data
    @Builder
    public static class CropInternalDto {
        private Integer id;
        private String cropName;
    }

    @Data
    @Builder
    public static class VarietyInternalDto {
        private Integer id;
        private String name;
        private Integer cropId;
    }
}
