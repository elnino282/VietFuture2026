package org.example.cropcatalog.mapper;

import org.example.cropcatalog.dto.request.CropRequest;
import org.example.cropcatalog.dto.response.CropResponse;
import org.example.cropcatalog.entity.Crop;
import org.example.cropcatalog.controller.InternalCropController.CropInternalDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.MappingConstants;

import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CropMapper {

    Crop toEntity(CropRequest request);

    void update(@MappingTarget Crop crop, CropRequest request);

    CropResponse toResponse(Crop crop);

    CropInternalDto toInternalDto(Crop crop);
}
