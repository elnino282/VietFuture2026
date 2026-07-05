package org.example.farm.mapper;

import org.example.farm.controller.InternalFarmController.FarmDetailDto;
import org.example.farm.controller.InternalFarmController.FarmSummaryDto;
import org.example.farm.entity.Farm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FarmMapper {

    @Mapping(target = "provinceName", source = "province.name")
    @Mapping(target = "wardName", source = "ward.name")
    FarmSummaryDto toSummaryDto(Farm farm);

    @Mapping(target = "provinceName", source = "province.name")
    @Mapping(target = "wardName", source = "ward.name")
    FarmDetailDto toDetailDto(Farm farm);
}
