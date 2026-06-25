package org.example.farm.service;

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
import org.example.farm.entity.Province;
import org.example.farm.entity.Ward;
import org.example.farm.exception.AppException;
import org.example.farm.exception.ErrorCode;
import org.example.farm.repository.ProvinceRepository;
import org.example.farm.repository.WardRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressImportService {

    ProvinceRepository provinceRepository;
    WardRepository wardRepository;

    private static final long MIN_EXPECTED_PROVINCES = 30;
    private static final long MIN_EXPECTED_WARDS = 1000;

    private static final Pattern PROVINCE_VALUES = Pattern.compile(
            "\\((\\d+),\\s*['\"]([^'\"]*)['\"],\\s*['\"]([^'\"]*)['\"],\\s*['\"]([^'\"]*)['\"],\\s*['\"]([^'\"]*)['\"]\\)");
    private static final Pattern WARD_VALUES = Pattern.compile(
            "\\((\\d+),\\s*['\"]([^'\"]*)['\"],\\s*['\"]([^'\"]*)['\"],\\s*['\"]([^'\"]*)['\"],\\s*['\"]([^'\"]*)['\"],\\s*(\\d+)\\)");

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void importOnStartupIfEmpty() {
        try {
            long provinceCount = provinceRepository.count();
            long wardCount = wardRepository.count();

            boolean shouldSyncAddressData = provinceCount < MIN_EXPECTED_PROVINCES || wardCount < MIN_EXPECTED_WARDS;
            if (shouldSyncAddressData) {
                log.info(
                        "Address tables are incomplete (provinces={}, wards={}). Starting synchronization from loc.sql...",
                        provinceCount,
                        wardCount);
                ClassPathResource resource = new ClassPathResource("loc.sql");
                if (resource.exists()) {
                    ImportResult result = doImportFromSqlFile(resource.getInputStream());
                    log.info("Address synchronization completed: +{} provinces, +{} wards",
                            result.getProvincesImported(), result.getWardsImported());
                } else {
                    log.warn("loc.sql not found in classpath. Skipping automatic import.");
                }
            } else {
                log.info("Address tables already complete (provinces={}, wards={}). Skipping automatic import.",
                        provinceCount,
                        wardCount);
            }
        } catch (Exception e) {
            log.error("Failed to import address data on startup: {}", e.getMessage(), e);
        }
    }

    @Transactional
    @CacheEvict(value = { "provinces", "province", "wards", "ward" }, allEntries = true)
    public ImportResult importFromSqlFile(InputStream inputStream) {
        return doImportFromSqlFile(inputStream);
    }

    private ImportResult doImportFromSqlFile(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            long provinceCountBefore = provinceRepository.count();
            long wardCountBefore = wardRepository.count();

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }
            String sqlContent = sql.toString();

            List<Province> provinces = parseProvinces(sqlContent);
            provinceRepository.saveAll(provinces);

            Map<Integer, Province> provinceMap = new HashMap<>();
            provinceRepository.findAll().forEach(p -> provinceMap.put(p.getId(), p));

            List<Ward> wards = parseWards(sqlContent, provinceMap);
            wardRepository.saveAll(wards);

            int provincesImported = (int) Math.max(0, provinceRepository.count() - provinceCountBefore);
            int wardsImported = (int) Math.max(0, wardRepository.count() - wardCountBefore);

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

    @Transactional
    @CacheEvict(value = { "provinces", "province", "wards", "ward" }, allEntries = true)
    public ImportResult forceReimport(InputStream inputStream) {
        log.warn("Force reimport requested. Clearing existing address data...");
        wardRepository.deleteAll();
        provinceRepository.deleteAll();
        return doImportFromSqlFile(inputStream);
    }

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

    @lombok.Builder
    @lombok.Data
    public static class ImportResult {
        private int provincesImported;
        private int wardsImported;
        private boolean success;
        private String message;
    }
}
