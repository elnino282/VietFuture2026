import httpClient from "@/shared/api/http";

/**
 * Current Weather API Response
 * (shape mirrored from backend weather payload)
 */
export interface LocationSuggestion {
    id: number;
    name: string;
    region: string;
    country: string;
    lat: number;
    lon: number;
    url: string;
}

/**
 * Current Weather API Response
 * (shape mirrored from backend weather payload)
 */
export interface CurrentWeatherResponse {
    location: {
        name: string;
        region: string;
        country: string;
        lat: number;
        lon: number;
        tz_id: string;
        localtime: string;
    };
    current: {
        last_updated: string;
        temp_c: number;
        feelslike_c: number;
        condition: {
            text: string;
            icon: string;
            code: number;
        };
        wind_kph: number;
        wind_dir: string;
        pressure_mb: number;
        precip_mm: number;
        humidity: number;
        cloud: number;
        vis_km: number;
        uv: number;
    };
}

/**
 * Forecast Day in API Response
 */
export interface ForecastDayResponse {
    date: string;
    day: {
        maxtemp_c: number;
        mintemp_c: number;
        avgtemp_c: number;
        maxwind_kph: number;
        totalprecip_mm: number;
        avghumidity: number;
        daily_chance_of_rain: number;
        condition: {
            text: string;
            icon: string;
            code: number;
        };
    };
    astro: {
        sunrise: string;
        sunset: string;
    };
}

/**
 * Forecast API payload used by mapper
 */
export interface ForecastWeatherResponse {
    location: {
        name: string;
        region: string;
        country: string;
        lat: number;
        lon: number;
        tz_id: string;
        localtime: string;
    };
    current: CurrentWeatherResponse["current"];
    forecast: {
        forecastday: ForecastDayResponse[];
    };
}

export type DashboardWeatherStatus =
    | "SUCCESS"
    | "LOCATION_REQUIRED"
    | "WEATHER_UNAVAILABLE";

export interface DashboardWeatherResponse {
    status: DashboardWeatherStatus;
    message: string;
    farmId: number | null;
    seasonId: number | null;
    farmName: string | null;
    location: {
        latitude: number | null;
        longitude: number | null;
        displayName: string | null;
    } | null;
    weather: ForecastWeatherResponse | null;
}

interface ApiEnvelope<T> {
    status: number;
    code: string;
    message: string;
    result: T;
}

export interface GetDashboardWeatherParams {
    farmId?: number;
    seasonId?: number;
}

/**
 * Fetch weather summary through backend dashboard endpoint
 */
export async function getDashboardWeather(
    params?: GetDashboardWeatherParams
): Promise<DashboardWeatherResponse> {
    const response = await httpClient.get<ApiEnvelope<DashboardWeatherResponse>>(
        "/api/v1/dashboard/weather",
        { params }
    );
    return response.data.result;
}
