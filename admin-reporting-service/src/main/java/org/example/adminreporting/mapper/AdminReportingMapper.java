package org.example.adminreporting.mapper;

import org.example.adminreporting.dto.response.*;
import org.example.adminreporting.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface AdminReportingMapper {

    @Mapping(target = "seasonName", source = "seasonName")
    @Mapping(target = "title", expression = "java(\"Task \" + task.getTaskId())")
    @Mapping(target = "description", expression = "java(\"Task details for season \" + task.getSeasonId())")
    TaskResponse toTaskResponse(TaskSummary task, String seasonName);

    @Mapping(target = "id", source = "farmId")
    @Mapping(target = "name", source = "farmName")
    FarmResponse toFarmResponse(FarmSummary farmSummary);

    @Mapping(target = "id", source = "plot.plotId")
    @Mapping(target = "farmName", source = "farmName")
    @Mapping(target = "status", constant = "ACTIVE")
    PlotResponse toPlotResponse(PlotSummary plot, String farmName);

    @Mapping(target = "id", source = "seasonId")
    @Mapping(target = "expectedYieldKg", source = "expectedYieldKg")
    @Mapping(target = "actualYieldKg", source = "actualYieldKg")
    SeasonResponse toSeasonResponse(SeasonSummary seasonSummary);

    @Mapping(target = "id", source = "season.seasonId")
    @Mapping(target = "plotName", source = "plotName")
    @Mapping(target = "cropName", source = "season.cropName")
    @Mapping(target = "varietyName", source = "season.varietyName")
    SeasonDetailResponse toSeasonDetailResponse(SeasonSummary season, String plotName);

    @Mapping(target = "seasonName", source = "seasonName")
    @Mapping(target = "incidentType", constant = "PEST")
    @Mapping(target = "severity", constant = "MEDIUM")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    IncidentResponse toIncidentResponse(IncidentSummary incidentSummary, String seasonName);
}
