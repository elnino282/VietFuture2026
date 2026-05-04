package org.example.QuanLyMuaVu.module.ai.controller;


import java.math.BigDecimal;
import java.util.Map;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AIController {

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/farmer/ai/suggestions")
    public ApiResponse<Map<String, Object>> suggestions(@RequestParam(required = false) String crop,
                                                        @RequestParam(required = false) String soil,
                                                        @RequestParam(required = false) String season) {
        Map<String, Object> payload = Map.of(
                "crop", crop,
                "soil", soil,
                "season", season,
                "suggestions", new String[]{
                        "Use drip irrigation to optimize water",
                        "Apply NPK 16-16-8 at early growth"
                }
        );
        return ApiResponse.success(payload);
    }

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/farmer/ai/predict-yield")
    public ApiResponse<Map<String, Object>> predictYield(@RequestParam BigDecimal area,
                                                         @RequestParam String crop) {
        BigDecimal estimated = area.multiply(BigDecimal.valueOf(2.5));
        Map<String, Object> payload = Map.of(
                "crop", crop,
                "area", area,
                "estimatedYieldTon", estimated
        );
        return ApiResponse.success(payload);
    }

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/farmer/ai/optimize-cost")
    public ApiResponse<Map<String, Object>> optimizeCost(@RequestParam BigDecimal budget,
                                                         @RequestParam String crop) {
        Map<String, Object> payload = Map.of(
                "crop", crop,
                "budget", budget,
                "plan", new String[]{
                        "Allocate 40% to fertilizers",
                        "Allocate 30% to irrigation",
                        "Allocate 30% to pest control"
                }
        );
        return ApiResponse.success(payload);
    }

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/ai/qa")
    public ApiResponse<Map<String, Object>> qa(@RequestParam String question) {
        Map<String, Object> payload = Map.of(
                "question", question,
                "answer", "AI agronomy Q&A placeholder answer for: " + question
        );
        return ApiResponse.success(payload);
    }
}
