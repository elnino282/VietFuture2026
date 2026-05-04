package org.example.QuanLyMuaVu.module.season.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.port.SeasonCommandPort;
import org.example.QuanLyMuaVu.module.season.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonCommandService implements SeasonCommandPort {

    SeasonRepository seasonRepository;

    @Override
    public Season saveSeason(Season season) {
        return seasonRepository.save(season);
    }
}
