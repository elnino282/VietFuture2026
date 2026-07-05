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
    private final org.example.cropcatalog.mapper.CropMapper cropMapper;
    private final org.example.cropcatalog.mapper.VarietyMapper varietyMapper;

    @GetMapping("/crops/{id}")
    public ResponseEntity<CropInternalDto> getCropInternal(@PathVariable Integer id) {
        Crop crop = cropRepository.findById(id).orElse(null);
        if (crop == null) {
            return ResponseEntity.notFound().build();
        }
        CropInternalDto dto = cropMapper.toInternalDto(crop);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/varieties/{id}")
    public ResponseEntity<VarietyInternalDto> getVarietyInternal(@PathVariable Integer id) {
        Variety variety = varietyRepository.findById(id).orElse(null);
        if (variety == null) {
            return ResponseEntity.notFound().build();
        }
        VarietyInternalDto dto = varietyMapper.toInternalDto(variety);
        return ResponseEntity.ok(dto);
    }

    @Data
    public static class CropInternalDto {
        private Integer id;
        private String cropName;
    }

    @Data
    public static class VarietyInternalDto {
        private Integer id;
        private String name;
        private Integer cropId;
    }
}
