import { useState, useCallback, useEffect } from "react";
import {
    Sun,
    CloudRain,
    Wind,
    Snowflake,
    AlertTriangle,
} from "lucide-react";
import type {
    UseWeatherWidgetReturn,
    SprayConditions,
    SoilMoistureInfo,
    WeatherData,
    ForecastDay,
    LocationSuggestion,
    WeatherWidgetDataState,
} from "../types";
import {
    DEFAULT_AGRI_ALERTS,
    API_CONFIG,
    UV_INDEX,
    UV_COLORS,
    UV_LABELS,
    SPRAY_CONDITIONS,
    SPRAY_COLORS,
    SOIL_MOISTURE,
    SOIL_COLORS,
    ALERT_COLORS,
} from "../constants";
import { getDashboardWeather } from "../services/weatherApi";
import {
    mapForecastToWeatherData,
    mapForecastDays,
    generateAgriAlerts,
} from "../utils/weatherMapper";

interface UseWeatherWidgetOptions {
    farmId?: number;
    seasonId?: number | null;
}

/**
 * Custom Hook: Weather Widget Controller
 * Uses backend weather endpoint as single source of truth.
 */
export function useWeatherWidget(
    options: UseWeatherWidgetOptions = {}
): UseWeatherWidgetReturn {
    const { farmId, seasonId } = options;

    // Location State (read-only from backend farm resolution)
    const [location, setLocation] = useState<string | null>(null);
    const [isEditingLocation, setIsEditingLocation] = useState(false);
    const [tempLocation, setTempLocation] = useState("");
    const [locationSuggestions] = useState<LocationSuggestion[]>([]);
    const [isSearchingLocations] = useState(false);

    // Loading/Error State
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [uiState, setUiState] = useState<WeatherWidgetDataState>("loading");
    const [statusMessage, setStatusMessage] = useState<string | null>(null);

    // Weather Data State
    const [weatherData, setWeatherData] = useState<WeatherData | null>(null);
    const [forecast, setForecast] = useState<ForecastDay[]>([]);
    const [agriAlerts, setAgriAlerts] = useState(DEFAULT_AGRI_ALERTS);

    const clearWeatherData = useCallback(() => {
        setWeatherData(null);
        setForecast([]);
        setAgriAlerts(DEFAULT_AGRI_ALERTS);
    }, []);

    const fetchWeatherData = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        setUiState("loading");
        setStatusMessage(null);

        try {
            const response = await getDashboardWeather({
                farmId,
                seasonId: seasonId ?? undefined,
            });

            const resolvedLocation =
                response.location?.displayName ||
                response.farmName ||
                API_CONFIG.DEFAULT_LOCATION;
            setLocation(resolvedLocation);
            setTempLocation(resolvedLocation);

            if (response.status === "LOCATION_REQUIRED") {
                clearWeatherData();
                setUiState("location_required");
                setStatusMessage(
                    response.message || "Please set farm coordinates before using weather dashboard."
                );
                return;
            }

            if (response.status === "WEATHER_UNAVAILABLE") {
                clearWeatherData();
                setUiState("weather_unavailable");
                setStatusMessage(
                    response.message || "Weather service is temporarily unavailable."
                );
                return;
            }

            if (!response.weather) {
                clearWeatherData();
                setUiState("weather_unavailable");
                setStatusMessage("Weather service returned no data.");
                return;
            }

            const mappedWeatherData = mapForecastToWeatherData(response.weather);
            setWeatherData(mappedWeatherData);

            const mappedForecast = mapForecastDays(response.weather.forecast.forecastday, true);
            setForecast(mappedForecast);

            const alerts = generateAgriAlerts(mappedWeatherData);
            setAgriAlerts(alerts);

            setUiState("success");
            setStatusMessage(null);
        } catch (err) {
            console.error("Error fetching weather data:", err);
            clearWeatherData();
            const message =
                err instanceof Error ? err.message : "Failed to fetch weather data";
            setUiState("error");
            setStatusMessage(message);
            setError(message);
        } finally {
            setIsLoading(false);
        }
    }, [clearWeatherData, farmId, seasonId]);

    useEffect(() => {
        void fetchWeatherData();
    }, [fetchWeatherData]);

    /**
     * Location editing is intentionally disabled.
     * Weather location must come from backend farm coordinates.
     */
    const handleSaveLocation = useCallback(async () => {
        setIsEditingLocation(false);
        setError("Location is managed by farm settings. Please update farm coordinates.");
    }, []);

    const handleCancelLocation = useCallback(() => {
        setTempLocation(location || "");
        setIsEditingLocation(false);
        setError(null);
    }, [location]);

    const handleRefresh = useCallback(async () => {
        await fetchWeatherData();
    }, [fetchWeatherData]);

    const getUVIndexColor = useCallback((index: number): string => {
        if (index <= UV_INDEX.LOW_THRESHOLD) return UV_COLORS.LOW;
        if (index <= UV_INDEX.MODERATE_THRESHOLD) return UV_COLORS.MODERATE;
        if (index <= UV_INDEX.HIGH_THRESHOLD) return UV_COLORS.HIGH;
        return UV_COLORS.VERY_HIGH;
    }, []);

    const getUVIndexLabel = useCallback((index: number): string => {
        if (index <= UV_INDEX.LOW_THRESHOLD) return UV_LABELS.LOW;
        if (index <= UV_INDEX.MODERATE_THRESHOLD) return UV_LABELS.MODERATE;
        if (index <= UV_INDEX.HIGH_THRESHOLD) return UV_LABELS.HIGH;
        return UV_LABELS.VERY_HIGH;
    }, []);

    const getSprayConditions = useCallback((): SprayConditions => {
        if (!weatherData) {
            return {
                status: "fair",
                color: SPRAY_COLORS.FAIR,
                label: "Unknown",
            };
        }

        const { windSpeed, humidity, temperature } = weatherData;

        if (windSpeed > SPRAY_CONDITIONS.MAX_WIND_SPEED) {
            return {
                status: "poor",
                color: SPRAY_COLORS.POOR,
                label: "Too Windy",
            };
        }

        if (temperature > SPRAY_CONDITIONS.MAX_TEMPERATURE) {
            return {
                status: "poor",
                color: SPRAY_COLORS.POOR,
                label: "Too Hot",
            };
        }

        if (
            windSpeed < SPRAY_CONDITIONS.MIN_WIND_SPEED &&
            humidity >= SPRAY_CONDITIONS.MIN_HUMIDITY &&
            humidity <= SPRAY_CONDITIONS.MAX_HUMIDITY
        ) {
            return {
                status: "excellent",
                color: SPRAY_COLORS.EXCELLENT,
                label: "Excellent",
            };
        }

        return {
            status: "fair",
            color: SPRAY_COLORS.FAIR,
            label: "Fair",
        };
    }, [weatherData]);

    const getSoilMoistureStatus = useCallback(
        (moisture: number): SoilMoistureInfo => {
            if (moisture < SOIL_MOISTURE.DRY_THRESHOLD) return SOIL_COLORS.DRY;
            if (moisture > SOIL_MOISTURE.WET_THRESHOLD) return SOIL_COLORS.WET;
            return SOIL_COLORS.OPTIMAL;
        },
        []
    );

    const getAlertIcon = useCallback((type: string): JSX.Element => {
        switch (type) {
            case "frost":
                return <Snowflake className="w-4 h-4" />;
            case "heat":
                return <Sun className="w-4 h-4" />;
            case "wind":
                return <Wind className="w-4 h-4" />;
            case "rain":
                return <CloudRain className="w-4 h-4" />;
            default:
                return <AlertTriangle className="w-4 h-4" />;
        }
    }, []);

    const getAlertColor = useCallback((severity: string): string => {
        switch (severity) {
            case "high":
                return ALERT_COLORS.HIGH;
            case "medium":
                return ALERT_COLORS.MEDIUM;
            case "low":
                return ALERT_COLORS.LOW;
            default:
                return "";
        }
    }, []);

    const handleSetTempLocation = useCallback((value: string) => {
        setTempLocation(value);
    }, []);

    const handleSetIsEditingLocation = useCallback((value: boolean) => {
        if (value) {
            setError("Location is managed by farm settings. Please update farm coordinates.");
        }
        setIsEditingLocation(false);
    }, []);

    return {
        // Weather Data
        weatherData,
        forecast,
        agriAlerts,

        // Location State
        location,
        isEditingLocation,
        tempLocation,
        locationSuggestions,
        isSearchingLocations,

        // Loading/Error State
        isLoading,
        error,
        uiState,
        statusMessage,

        // Handlers
        setTempLocation: handleSetTempLocation,
        setIsEditingLocation: handleSetIsEditingLocation,
        handleSaveLocation,
        handleCancelLocation,
        handleRefresh,
        setError,
        handleLocationSearch: () => undefined,
        clearLocationSuggestions: () => undefined,

        // Computed Values
        getUVIndexColor,
        getUVIndexLabel,
        getSprayConditions,
        getSoilMoistureStatus,
        getAlertIcon,
        getAlertColor,
    };
}
