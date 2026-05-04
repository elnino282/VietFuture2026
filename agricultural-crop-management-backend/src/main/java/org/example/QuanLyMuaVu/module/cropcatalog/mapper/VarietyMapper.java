package org.example.QuanLyMuaVu.module.cropcatalog.mapper;

import org.example.QuanLyMuaVu.module.cropcatalog.dto.request.VarietyRequest;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.VarietyResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety;
import org.springframework.stereotype.Component;

@Component
public class VarietyMapper {

    public Variety toEntity(VarietyRequest request, Crop crop) {
        if (request == null || crop == null) {
            return null;
        }
        return Variety.builder()
                .crop(crop)
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public void update(Variety variety, VarietyRequest request, Crop crop) {
        if (variety == null || request == null || crop == null) {
            return;
        }
        variety.setCrop(crop);
        variety.setName(request.getName());
        variety.setDescription(request.getDescription());
    }

    public VarietyResponse toResponse(Variety variety) {
        if (variety == null) {
            return null;
        }
        return VarietyResponse.builder()
                .id(variety.getId())
                .cropId(variety.getCrop() != null ? variety.getCrop().getId() : null)
                .cropName(variety.getCrop() != null ? variety.getCrop().getCropName() : null)
                .name(variety.getName())
                .description(variety.getDescription())
                .build();
    }
}



