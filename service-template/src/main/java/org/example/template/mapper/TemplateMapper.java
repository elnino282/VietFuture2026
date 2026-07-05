package org.example.template.mapper;

import org.example.template.dto.TemplateResponse;
import org.example.template.entity.TemplateRecord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TemplateMapper {
    TemplateResponse toResponse(TemplateRecord entity);
}
