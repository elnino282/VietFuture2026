package org.example.QuanLyMuaVu.module.sustainability.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherForecastPayload {
    Location location;
    Current current;
    Forecast forecast;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        String name;
        String region;
        String country;
        Double lat;
        Double lon;
        String tz_id;
        String localtime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Current {
        String last_updated;
        double temp_c;
        double feelslike_c;
        Condition condition;
        double wind_kph;
        String wind_dir;
        double pressure_mb;
        double precip_mm;
        int humidity;
        int cloud;
        double vis_km;
        double uv;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Forecast {
        List<ForecastDay> forecastday;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastDay {
        String date;
        Day day;
        Astro astro;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Day {
        double maxtemp_c;
        double mintemp_c;
        double avgtemp_c;
        double maxwind_kph;
        double totalprecip_mm;
        double avghumidity;
        int daily_chance_of_rain;
        Condition condition;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Astro {
        String sunrise;
        String sunset;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        String text;
        String icon;
        int code;
    }
}
