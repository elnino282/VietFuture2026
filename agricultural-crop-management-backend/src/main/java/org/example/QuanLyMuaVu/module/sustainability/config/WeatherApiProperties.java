package org.example.QuanLyMuaVu.module.sustainability.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "weather.api")
@Getter
@Setter
public class WeatherApiProperties {

    private String baseUrl = "https://api.weatherapi.com/v1";
    private String key;
    private int forecastDays = 4;
    private long timeoutMs = 8000L;

    public boolean isConfigured() {
        return key != null && !key.isBlank();
    }
}
