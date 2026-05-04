package org.example.QuanLyMuaVu.module.season.mapper;

import java.math.BigDecimal;
import org.example.QuanLyMuaVu.module.season.dto.request.HarvestRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestResponse;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class HarvestMapper {
    public void update(Harvest harvest, HarvestRequest request) {
        if (harvest == null || request == null)
            return;
        harvest.setHarvestDate(request.getHarvestDate());
        harvest.setQuantity(request.getQuantity());
        harvest.setUnit(request.getUnit());
        harvest.setGrade(request.getGrade());
        harvest.setNote(request.getNote());
    }

    public HarvestResponse toResponse(Harvest harvest) {
        if (harvest == null)
            return null;
        BigDecimal revenue = null;
        if (harvest.getQuantity() != null && harvest.getUnit() != null) {
            revenue = harvest.getQuantity().multiply(harvest.getUnit());
        }
        return HarvestResponse.builder()
                .id(harvest.getId())
                .seasonId(harvest.getSeason() != null ? harvest.getSeason().getId() : null)
                .seasonName(harvest.getSeason() != null ? harvest.getSeason().getSeasonName() : null)
                .harvestDate(harvest.getHarvestDate())
                .quantity(harvest.getQuantity())
                .unit(harvest.getUnit())
                .grade(harvest.getGrade())
                .revenue(revenue)
                .note(harvest.getNote())
                .createdAt(harvest.getCreatedAt())
                .build();
    }
}
