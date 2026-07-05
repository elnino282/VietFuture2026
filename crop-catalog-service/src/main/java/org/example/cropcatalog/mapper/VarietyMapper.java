package org.example.cropcatalog.mapper;

import org.example.cropcatalog.dto.request.VarietyRequest;
import org.example.cropcatalog.dto.response.VarietyResponse;
import org.example.cropcatalog.entity.Crop;
import org.example.cropcatalog.entity.Variety;
import org.example.cropcatalog.controller.InternalCropController.VarietyInternalDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.MappingConstants;

import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface VarietyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "description", source = "request.description")
    Variety toEntity(VarietyRequest request, Crop crop);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "description", source = "request.description")
    void update(@MappingTarget Variety variety, VarietyRequest request, Crop crop);

    @Mapping(target = "cropId", source = "crop.id")
    @Mapping(target = "cropName", source = "crop.cropName")
    VarietyResponse toResponse(Variety variety);

    @Mapping(target = "cropId", source = "crop.id")
    VarietyInternalDto toInternalDto(Variety variety);
}
