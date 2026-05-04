package org.example.QuanLyMuaVu.module.identity.service;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.identity.dto.request.PreferencesUpdateRequest;
import org.example.QuanLyMuaVu.module.identity.dto.response.PreferencesResponse;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.entity.UserPreference;
import org.example.QuanLyMuaVu.module.identity.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class PreferencesService {

    String DEFAULT_CURRENCY = "VND";
    String DEFAULT_WEIGHT_UNIT = "KG";
    String DEFAULT_LOCALE = "vi-VN";

    Set<String> SUPPORTED_CURRENCIES = Set.of("VND", "USD");
    Set<String> SUPPORTED_WEIGHT_UNITS = Set.of("KG", "G", "TON");
    Set<String> SUPPORTED_LOCALES = Set.of("vi-VN", "en-US");

    UserPreferenceRepository userPreferenceRepository;
    CurrentUserService currentUserService;

    public PreferencesResponse getMyPreferences() {
        UserPreference preference = getOrCreatePreference();
        return toResponse(preference);
    }

    public PreferencesResponse updateMyPreferences(PreferencesUpdateRequest request) {
        UserPreference preference = getOrCreatePreference();

        if (request != null) {
            if (request.getCurrency() != null) {
                preference.setCurrencyCode(validateCurrency(request.getCurrency()));
            }
            if (request.getWeightUnit() != null) {
                preference.setWeightUnit(validateWeightUnit(request.getWeightUnit()));
            }
            if (request.getLocale() != null) {
                preference.setLocale(validateLocale(request.getLocale()));
            }
        }

        UserPreference saved = userPreferenceRepository.save(preference);
        return toResponse(saved);
    }

    private UserPreference getOrCreatePreference() {
        Long userId = currentUserService.getCurrentUserId();
        User user = currentUserService.getCurrentUser();

        UserPreference preference = userPreferenceRepository.findByUser_Id(userId)
                .orElseGet(() -> userPreferenceRepository.save(UserPreference.builder()
                        .user(user)
                        .currencyCode(DEFAULT_CURRENCY)
                        .weightUnit(DEFAULT_WEIGHT_UNIT)
                        .locale(DEFAULT_LOCALE)
                        .build()));

        boolean needsNormalization = false;

        String normalizedCurrency = normalizeCurrency(preference.getCurrencyCode());
        if (!Objects.equals(normalizedCurrency, preference.getCurrencyCode())) {
            preference.setCurrencyCode(normalizedCurrency);
            needsNormalization = true;
        }

        String normalizedWeightUnit = normalizeWeightUnit(preference.getWeightUnit());
        if (!Objects.equals(normalizedWeightUnit, preference.getWeightUnit())) {
            preference.setWeightUnit(normalizedWeightUnit);
            needsNormalization = true;
        }

        String normalizedLocale = normalizeLocale(preference.getLocale());
        if (!Objects.equals(normalizedLocale, preference.getLocale())) {
            preference.setLocale(normalizedLocale);
            needsNormalization = true;
        }

        if (needsNormalization) {
            preference = userPreferenceRepository.save(preference);
        }

        return preference;
    }

    private PreferencesResponse toResponse(UserPreference preference) {
        return PreferencesResponse.builder()
                .currency(normalizeCurrency(preference.getCurrencyCode()))
                .weightUnit(normalizeWeightUnit(preference.getWeightUnit()))
                .locale(normalizeLocale(preference.getLocale()))
                .build();
    }

    private String validateCurrency(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(normalized)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String validateWeightUnit(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_WEIGHT_UNITS.contains(normalized)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String validateLocale(String value) {
        String normalized = normalizeLocale(value);
        if (!SUPPORTED_LOCALES.contains(normalized)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED_CURRENCIES.contains(normalized) ? normalized : DEFAULT_CURRENCY;
    }

    private String normalizeWeightUnit(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_WEIGHT_UNIT;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED_WEIGHT_UNITS.contains(normalized) ? normalized : DEFAULT_WEIGHT_UNIT;
    }

    private String normalizeLocale(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LOCALE;
        }

        String compact = value.trim().replace('_', '-');
        if (compact.equalsIgnoreCase("vi-vn")) {
            return "vi-VN";
        }
        if (compact.equalsIgnoreCase("en-us")) {
            return "en-US";
        }

        return DEFAULT_LOCALE;
    }
}
