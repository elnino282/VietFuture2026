package org.example.QuanLyMuaVu.Service.Sustainability;

import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.module.sustainability.dto.response.SustainabilityOverviewResponse;
import org.example.QuanLyMuaVu.module.sustainability.service.SustainabilityRecommendationService;
import org.example.QuanLyMuaVu.Enums.NutrientInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SustainabilityRecommendationServiceTest {

    private SustainabilityRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new SustainabilityRecommendationService(new AppProperties());
    }

    @Test
    @DisplayName("High mineral dependency with strong confidence suggests controlled 20% reduction")
    void generate_WhenMineralHighAndConfidenceStrong_ShouldSuggestTwentyPercentRule() {
        List<String> result = service.generate(
                new BigDecimal("72"),
                new BigDecimal("65"),
                new BigDecimal("56"),
                new BigDecimal("18"),
                new BigDecimal("0.82"),
                sampleBreakdown(),
                sampleMethods("measured")
        );

        assertTrue(result.stream().anyMatch(item -> item.contains("20%")));
    }

    @Test
    @DisplayName("High mineral dependency with weak confidence avoids automatic 20% reduction")
    void generate_WhenConfidenceLow_ShouldAvoidAutomaticCutRecommendation() {
        List<String> result = service.generate(
                new BigDecimal("75"),
                new BigDecimal("64"),
                new BigDecimal("41"),
                new BigDecimal("30"),
                new BigDecimal("0.45"),
                sampleBreakdown(),
                sampleMethods("estimated")
        );

        assertTrue(result.stream().anyMatch(item -> item.contains("not strong enough")));
        assertFalse(result.stream().anyMatch(item -> item.contains("monitor yield response before scaling this rule")));
    }

    private SustainabilityOverviewResponse.InputsBreakdown sampleBreakdown() {
        return SustainabilityOverviewResponse.InputsBreakdown.builder()
                .mineralFertilizerN(new BigDecimal("70"))
                .organicFertilizerN(new BigDecimal("8"))
                .biologicalFixationN(new BigDecimal("3"))
                .irrigationWaterN(new BigDecimal("2"))
                .atmosphericDepositionN(new BigDecimal("5"))
                .seedImportN(BigDecimal.ZERO)
                .soilLegacyN(BigDecimal.ZERO)
                .controlSupplyN(BigDecimal.ZERO)
                .build();
    }

    private Map<NutrientInputSource, String> sampleMethods(String fallbackMethod) {
        Map<NutrientInputSource, String> methods = new LinkedHashMap<>();
        for (NutrientInputSource source : NutrientInputSource.values()) {
            methods.put(source, fallbackMethod);
        }
        return methods;
    }
}
