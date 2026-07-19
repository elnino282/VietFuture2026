package org.example.season.client;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class SustainabilityServiceClientFallback implements SustainabilityServiceClient {

    @Override
    public List<SoilTestInternalDto> getSoilTests(Integer seasonId) {
        return new ArrayList<>();
    }

    @Override
    public List<IrrigationWaterAnalysisInternalDto> getWaterAnalyses(Integer seasonId) {
        return new ArrayList<>();
    }

    @Override
    public List<NutrientInputEventInternalDto> getNutrientInputs(Integer seasonId) {
        return new ArrayList<>();
    }
}

