package org.example.QuanLyMuaVu.Config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for the application.
 * Uses simple in-memory caching for address data (countries, provinces, wards).
 * <p>
 * For production with high traffic, consider switching to Redis:
 * 1. Add spring-boot-starter-data-redis dependency
 * 2. Configure Redis connection in application.properties
 * 3. Replace ConcurrentMapCacheManager with RedisCacheManager
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure in-memory cache manager for address data.
     * Cache entries will persist until application restart or manual eviction.
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "countries", // All countries list
                "country", // Single country by ID
                "provinces", // All provinces list
                "province", // Single province by ID
                "wards", // Wards by province ID
                "ward" // Single ward by ID
        );
    }
}
