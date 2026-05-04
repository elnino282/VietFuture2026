package org.example.QuanLyMuaVu.module.season.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.module.season.dto.request.HarvestRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestResponse;
import org.example.QuanLyMuaVu.module.season.entity.Harvest;
import org.example.QuanLyMuaVu.module.season.mapper.HarvestMapper;
import org.example.QuanLyMuaVu.module.season.repository.HarvestRepository;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HarvestService {

    private final HarvestRepository harvestRepository;
    private final SeasonRepository seasonRepository;
    private final HarvestMapper harvestMapper;

    public HarvestResponse create(HarvestRequest request) {
        var season = seasonRepository.findById(request.getSeasonId()).orElseThrow();
        Harvest harvest = Harvest.builder().season(season).build();
        harvestMapper.update(harvest, request);
        harvest = harvestRepository.save(harvest);
        return harvestMapper.toResponse(harvest);
    }

    public List<HarvestResponse> getAll() {
        return harvestRepository.findAll().stream().map(harvestMapper::toResponse).toList();
    }

    public HarvestResponse update(Integer id, HarvestRequest request) {
        Harvest harvest = harvestRepository.findById(id).orElseThrow();
        harvestMapper.update(harvest, request);
        return harvestMapper.toResponse(harvestRepository.save(harvest));
    }

    public void delete(Integer id) {
        harvestRepository.deleteById(id);
    }
}
