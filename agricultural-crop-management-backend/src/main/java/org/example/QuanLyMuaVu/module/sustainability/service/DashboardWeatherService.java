package org.example.QuanLyMuaVu.module.sustainability.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.sustainability.config.WeatherApiProperties;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.DashboardWeatherResponse;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.WeatherForecastPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardWeatherService {

    private final CurrentUserService currentUserService;
    private final FarmerOwnershipService ownershipService;
    private final SeasonQueryPort seasonQueryPort;
    private final WeatherApiProperties weatherApiProperties;
    private final WebClient.Builder webClientBuilder;

    public DashboardWeatherResponse getWeather(Integer farmId, Integer seasonId) {
        Long ownerId = currentUserService.getCurrentUserId();
        ResolvedFarmContext context = resolveFarmContext(ownerId, farmId, seasonId);

        if (context.farm() == null) {
            return DashboardWeatherResponse.builder()
                    .status(DashboardWeatherResponse.WeatherStatus.LOCATION_REQUIRED)
                    .message("No farm location found. Please add a farm and set coordinates.")
                    .build();
        }

        Farm farm = context.farm();
        if (farm.getLatitude() == null || farm.getLongitude() == null) {
            return DashboardWeatherResponse.builder()
                    .status(DashboardWeatherResponse.WeatherStatus.LOCATION_REQUIRED)
                    .message("Farm coordinates are missing. Please update farm location.")
                    .farmId(farm.getId())
                    .seasonId(context.seasonId())
                    .farmName(farm.getName())
                    .location(buildFarmLocation(farm))
                    .build();
        }

        if (!weatherApiProperties.isConfigured()) {
            log.warn("Weather API key is not configured on backend.");
            return DashboardWeatherResponse.builder()
                    .status(DashboardWeatherResponse.WeatherStatus.WEATHER_UNAVAILABLE)
                    .message("Weather service is not configured.")
                    .farmId(farm.getId())
                    .seasonId(context.seasonId())
                    .farmName(farm.getName())
                    .location(buildFarmLocation(farm))
                    .build();
        }

        try {
            WeatherForecastPayload payload = fetchWeatherForecast(farm.getLatitude(), farm.getLongitude());
            if (payload == null || payload.getCurrent() == null || payload.getForecast() == null) {
                return DashboardWeatherResponse.builder()
                        .status(DashboardWeatherResponse.WeatherStatus.WEATHER_UNAVAILABLE)
                        .message("Weather data is temporarily unavailable.")
                        .farmId(farm.getId())
                        .seasonId(context.seasonId())
                        .farmName(farm.getName())
                        .location(buildFarmLocation(farm))
                        .build();
            }

            return DashboardWeatherResponse.builder()
                    .status(DashboardWeatherResponse.WeatherStatus.SUCCESS)
                    .message("OK")
                    .farmId(farm.getId())
                    .seasonId(context.seasonId())
                    .farmName(farm.getName())
                    .location(buildFarmLocation(farm))
                    .weather(payload)
                    .build();
        } catch (Exception exception) {
            log.warn("Unable to fetch weather for farmId={} seasonId={}: {}",
                    farm.getId(),
                    context.seasonId(),
                    exception.getMessage());
            return DashboardWeatherResponse.builder()
                    .status(DashboardWeatherResponse.WeatherStatus.WEATHER_UNAVAILABLE)
                    .message("Weather data is temporarily unavailable.")
                    .farmId(farm.getId())
                    .seasonId(context.seasonId())
                    .farmName(farm.getName())
                    .location(buildFarmLocation(farm))
                    .build();
        }
    }

    private ResolvedFarmContext resolveFarmContext(Long ownerId, Integer farmId, Integer seasonId) {
        if (farmId != null) {
            Farm requestedFarm = ownershipService.requireOwnedFarm(farmId);
            return new ResolvedFarmContext(requestedFarm, null);
        }

        if (seasonId != null) {
            org.example.QuanLyMuaVu.module.season.entity.Season season = ownershipService.requireOwnedSeason(seasonId);
            Farm seasonFarm = season.getPlot() != null ? season.getPlot().getFarm() : null;
            if (seasonFarm != null) {
                return new ResolvedFarmContext(seasonFarm, season.getId());
            }
        }

        List<org.example.QuanLyMuaVu.module.season.entity.Season> activeSeasons = seasonQueryPort
                .findActiveSeasonsByOwnerIdOrderByStartDateDesc(ownerId);
        for (org.example.QuanLyMuaVu.module.season.entity.Season season : activeSeasons) {
            Farm seasonFarm = season.getPlot() != null ? season.getPlot().getFarm() : null;
            if (seasonFarm != null) {
                return new ResolvedFarmContext(seasonFarm, season.getId());
            }
        }

        List<Farm> ownedFarms = ownershipService.getOwnedFarms();
        Comparator<Farm> farmOrder = Comparator.comparing(
                Farm::getId,
                Comparator.nullsLast(Integer::compareTo));
        Farm defaultFarm = ownedFarms.stream()
                .filter(farm -> Boolean.TRUE.equals(farm.getActive()))
                .min(farmOrder)
                .orElseGet(() -> ownedFarms.stream()
                        .min(farmOrder)
                        .orElse(null));

        return new ResolvedFarmContext(defaultFarm, seasonId);
    }

    private DashboardWeatherResponse.FarmLocation buildFarmLocation(Farm farm) {
        return DashboardWeatherResponse.FarmLocation.builder()
                .latitude(farm.getLatitude())
                .longitude(farm.getLongitude())
                .displayName(buildDisplayName(farm))
                .build();
    }

    private String buildDisplayName(Farm farm) {
        List<String> segments = new ArrayList<>();
        if (farm.getName() != null && !farm.getName().isBlank()) {
            segments.add(farm.getName().trim());
        }
        if (farm.getWard() != null && farm.getWard().getName() != null && !farm.getWard().getName().isBlank()) {
            segments.add(farm.getWard().getName().trim());
        }
        if (farm.getProvince() != null && farm.getProvince().getName() != null
                && !farm.getProvince().getName().isBlank()) {
            segments.add(farm.getProvince().getName().trim());
        }
        return String.join(", ", segments);
    }

    private WeatherForecastPayload fetchWeatherForecast(BigDecimal latitude, BigDecimal longitude) {
        String locationQuery = latitude.stripTrailingZeros().toPlainString()
                + ","
                + longitude.stripTrailingZeros().toPlainString();

        WebClient weatherClient = webClientBuilder
                .baseUrl(weatherApiProperties.getBaseUrl())
                .build();

        int forecastDays = Math.max(weatherApiProperties.getForecastDays(), 1);
        return weatherClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast.json")
                        .queryParam("key", weatherApiProperties.getKey())
                        .queryParam("q", locationQuery)
                        .queryParam("days", forecastDays)
                        .queryParam("aqi", "no")
                        .queryParam("alerts", "no")
                        .build())
                .retrieve()
                .bodyToMono(WeatherForecastPayload.class)
                .block(Duration.ofMillis(weatherApiProperties.getTimeoutMs()));
    }

    private record ResolvedFarmContext(Farm farm, Integer seasonId) {
    }
}
