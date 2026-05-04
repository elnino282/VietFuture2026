package org.example.QuanLyMuaVu.module.farm.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.entity.Province;
import org.example.QuanLyMuaVu.module.farm.entity.Ward;
import org.example.QuanLyMuaVu.module.farm.repository.ProvinceRepository;
import org.example.QuanLyMuaVu.module.farm.repository.WardRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for importing address data from SQL files.
 * Supports both automatic import on startup (if tables are empty) and manual
 * import via API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressImportService {

    ProvinceRepository provinceRepository;
    WardRepository wardRepository;

    // Regex patterns for parsing SQL INSERT statements
    private static final Pattern PROVINCE_VALUES = Pattern.compile(
            "\\((\\d+),\\s*'([^']*)',\\s*'([^']*)',\\s*'([^']*)',\\s*'([^']*)'\\)");
    private static final Pattern WARD_VALUES = Pattern.compile(
            "\\((\\d+),\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*(\\d+)\\)");

    /**
     * Automatically import address data on application startup if tables are empty.
     * Note: We perform the import logic directly here instead of delegating to
     * importFromSqlFile() to avoid Spring AOP proxy self-invocation issues which
     * would cause the transaction context to be lost.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void importOnStartupIfEmpty() {
        try {
            if (provinceRepository.count() == 0 || wardRepository.count() == 0) {
                log.info("Address tables are empty. Starting automatic import from loc.sql...");
                ClassPathResource resource = new ClassPathResource("loc.sql");
                if (resource.exists()) {
                    ImportResult result = doImportFromSqlFile(resource.getInputStream());
                    log.info("Address import completed: {} provinces, {} wards",
                            result.getProvincesImported(), result.getWardsImported());
                } else {
                    log.warn("loc.sql not found in classpath. Skipping automatic import.");
                }
            } else {
                log.info("Address tables already populated. Skipping automatic import.");
            }
        } catch (Exception e) {
            log.error("Failed to import address data on startup: {}", e.getMessage(), e);
        }
    }

    /**
     * Import address data from SQL file input stream.
     * This method can be called from API endpoint for manual import.
     */
    @Transactional
    @CacheEvict(value = { "provinces", "province", "wards", "ward" }, allEntries = true)
    public ImportResult importFromSqlFile(InputStream inputStream) {
        return doImportFromSqlFile(inputStream);
    }

    /**
     * Internal helper method that performs the actual SQL import logic.
     * This method is used internally to avoid Spring AOP proxy self-invocation
     * issues.
     */
    private ImportResult doImportFromSqlFile(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }
            String sqlContent = sql.toString();

            int provincesImported = 0;
            int wardsImported = 0;

            // Import provinces
            Map<Integer, Province> provinceMap = new HashMap<>();
            if (provinceRepository.count() == 0) {
                List<Province> provinces = parseProvinces(sqlContent);
                provinceRepository.saveAll(provinces);
                provincesImported = provinces.size();
                provinces.forEach(p -> provinceMap.put(p.getId(), p));
                log.debug("Imported {} provinces", provincesImported);
            } else {
                provinceRepository.findAll().forEach(p -> provinceMap.put(p.getId(), p));
            }

            // Import wards
            if (wardRepository.count() == 0) {
                List<Ward> wards = parseWards(sqlContent, provinceMap);
                wardRepository.saveAll(wards);
                wardsImported = wards.size();
                log.debug("Imported {} wards", wardsImported);
            }

            return ImportResult.builder()
                    .provincesImported(provincesImported)
                    .wardsImported(wardsImported)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error importing address data: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.ADDRESS_IMPORT_FAILED);
        }
    }

    /**
     * Force re-import by clearing existing data and importing fresh.
     * Use with caution - this will delete all existing address data.
     */
    @Transactional
    @CacheEvict(value = { "provinces", "province", "wards", "ward" }, allEntries = true)
    public ImportResult forceReimport(InputStream inputStream) {
        log.warn("Force reimport requested. Clearing existing address data...");
        wardRepository.deleteAll();
        provinceRepository.deleteAll();
        return doImportFromSqlFile(inputStream);
    }

    // ==================== PARSING METHODS ====================

    private List<Province> parseProvinces(String sql) {
        List<Province> provinces = new ArrayList<>();
        Matcher matcher = PROVINCE_VALUES.matcher(sql);

        while (matcher.find()) {
            try {
                Province province = Province.builder()
                        .id(Integer.parseInt(matcher.group(1)))
                        .name(matcher.group(2))
                        .slug(matcher.group(3))
                        .type(matcher.group(4))
                        .nameWithType(matcher.group(5))
                        .build();
                provinces.add(province);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse province entry: {}", matcher.group(0));
            }
        }
        return provinces;
    }

    private List<Ward> parseWards(String sql, Map<Integer, Province> provinceMap) {
        List<Ward> wards = new ArrayList<>();
        Matcher matcher = WARD_VALUES.matcher(sql);

        while (matcher.find()) {
            try {
                int provinceId = Integer.parseInt(matcher.group(6));
                Province province = provinceMap.get(provinceId);

                if (province != null) {
                    Ward ward = Ward.builder()
                            .id(Integer.parseInt(matcher.group(1)))
                            .name(matcher.group(2))
                            .slug(matcher.group(3))
                            .type(matcher.group(4))
                            .nameWithType(matcher.group(5))
                            .province(province)
                            .build();
                    wards.add(ward);
                } else {
                    log.warn("Province not found for ward: {} with provinceId: {}", matcher.group(2), provinceId);
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse ward entry: {}", matcher.group(0));
            }
        }
        return wards;
    }

    // ==================== RESULT DTO ====================

    @lombok.Builder
    @lombok.Data
    public static class ImportResult {
        private int provincesImported;
        private int wardsImported;
        private boolean success;
        private String message;
    }
}
