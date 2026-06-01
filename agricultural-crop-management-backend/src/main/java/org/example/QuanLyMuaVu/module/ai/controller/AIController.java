package org.example.QuanLyMuaVu.module.ai.controller;


import java.util.HashMap;
import java.math.BigDecimal;
import java.util.Map;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.ai.service.GeminiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AIController {

    private final GeminiService geminiService;

    public AIController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PreAuthorize("hasRole('BUYER')")
    @GetMapping("/ai/qa")
    public ApiResponse<Map<String, Object>> qa(@RequestParam String question,
                                               @RequestParam(required = false) String context) {
        String answer = geminiService.chatAsBuyerProcurementExpert(question, context);

        Map<String, Object> payload = new HashMap<>();
        payload.put("question", question);
        payload.put("answer", answer);
        if (context != null && !context.isBlank()) {
            payload.put("context", context);
        }
        return ApiResponse.success(payload);
    }
}
