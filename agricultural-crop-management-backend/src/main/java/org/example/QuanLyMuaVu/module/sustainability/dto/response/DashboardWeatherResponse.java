package org.example.QuanLyMuaVu.module.sustainability.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWeatherResponse {

    WeatherStatus status;
    String message;
    Integer farmId;
    Integer seasonId;
    String farmName;
    FarmLocation location;
    WeatherForecastPayload weather;

    public enum WeatherStatus {
        SUCCESS,
        LOCATION_REQUIRED,
        WEATHER_UNAVAILABLE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FarmLocation {
        BigDecimal latitude;
        BigDecimal longitude;
        String displayName;
    }
}
