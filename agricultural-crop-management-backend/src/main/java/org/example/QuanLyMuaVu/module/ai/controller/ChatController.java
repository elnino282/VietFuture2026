package org.example.QuanLyMuaVu.module.ai.controller;

import jakarta.validation.Valid;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.ai.dto.request.BuyerChatRequest;
import org.example.QuanLyMuaVu.module.ai.dto.request.ChatRequest;
import org.example.QuanLyMuaVu.module.ai.dto.response.BuyerChatResponse;
import org.example.QuanLyMuaVu.module.ai.dto.response.ChatResponse;
import org.example.QuanLyMuaVu.module.ai.service.GeminiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/farmer/ai/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String reply = geminiService.chatAsAgriculturalExpert(
                request.getUserMessage(),
                request.getCropContext()
        );

        ChatResponse response = ChatResponse.builder()
                .userMessage(request.getUserMessage())
                .cropContext(request.getCropContext())
                .assistantMessage(reply)
                .build();

        return ApiResponse.success(response);
    }

    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/buyer/ai/chat")
    public ApiResponse<BuyerChatResponse> buyerChat(@Valid @RequestBody BuyerChatRequest request) {
        String reply = geminiService.chatAsBuyerProcurementExpert(
                request.getUserMessage(),
                request.getBuyerContext()
        );

        BuyerChatResponse response = BuyerChatResponse.builder()
                .userMessage(request.getUserMessage())
                .buyerContext(request.getBuyerContext())
                .assistantMessage(reply)
                .build();

        return ApiResponse.success(response);
    }
}
