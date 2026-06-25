package org.example.season.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.entity.Season;
import org.example.season.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SeasonCommandService {

    SeasonRepository seasonRepository;

    public Season saveSeason(Season season) {
        return seasonRepository.save(season);
    }
}
