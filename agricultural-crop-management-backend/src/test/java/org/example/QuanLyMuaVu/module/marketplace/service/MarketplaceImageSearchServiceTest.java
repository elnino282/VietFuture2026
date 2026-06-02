package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.admin.service.AuditLogService;
import org.example.QuanLyMuaVu.module.ai.service.GeminiService;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceImageSearchResponse;
import org.example.QuanLyMuaVu.module.marketplace.repository.MarketplaceProductReviewRepository;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MarketplaceImageSearchServiceTest {

    @Mock
    GeminiService geminiService;

    @Mock
    MarketplaceProductReviewRepository marketplaceProductReviewRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    AuditLogService auditLogService;

    @Mock
    EntityManager entityManager;

    MarketplaceImageSearchService service;

    @BeforeEach
    void setUp() {
        service = new MarketplaceImageSearchService(
                geminiService,
                marketplaceProductReviewRepository,
                currentUserService,
                auditLogService,
                new ObjectMapper(),
                entityManager);
        when(currentUserService.getCurrentUserId()).thenReturn(42L);
    }

    @Test
    void analyze_withValidImage_returnsAnalysis() {
        when(geminiService.analyzeMarketplaceImage(any(byte[].class), eq("image/jpeg")))
                .thenReturn("""
                        {
                          "detectedProduct": "Cà chua",
                          "category": "Rau củ",
                          "keywordsVi": ["cà chua"],
                          "keywordsEn": ["tomato"],
                          "keywords": ["cà chua"],
                          "visualAttributes": ["màu đỏ"],
                          "confidence": 0.86,
                          "confidenceLabel": "high",
                          "agricultural": true,
                          "message": "Đã nhận diện cà chua."
                        }
                        """);

        var analysis = service.analyze(validJpeg());

        assertThat(analysis.detectedProduct()).isEqualTo("Cà chua");
        assertThat(analysis.agricultural()).isTrue();
        assertThat(analysis.confidence()).isEqualTo(0.86D);
        assertThat(analysis.keywordsVi()).contains("cà chua");
        verify(geminiService).analyzeMarketplaceImage(any(byte[].class), eq("image/jpeg"));
    }

    @Test
    void analyze_withUnsupportedMime_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "not-an-image".getBytes());

        AppException exception = assertThrows(AppException.class, () -> service.analyze(file));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MARKETPLACE_IMAGE_SEARCH_UNSUPPORTED_TYPE);
        verifyNoInteractions(geminiService);
    }

    @Test
    void analyze_withEmptyFile_throwsInvalidImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        AppException exception = assertThrows(AppException.class, () -> service.analyze(file));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MARKETPLACE_IMAGE_SEARCH_INVALID_IMAGE);
        verifyNoInteractions(geminiService);
    }

    @Test
    void analyze_withOversizedFile_throwsImageTooLarge() {
        byte[] bytes = new byte[(3 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                bytes);

        AppException exception = assertThrows(AppException.class, () -> service.analyze(file));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MARKETPLACE_IMAGE_SEARCH_IMAGE_TOO_LARGE);
        verifyNoInteractions(geminiService);
    }

    @Test
    void search_whenGeminiUnavailable_throwsFriendlyErrorAndDoesNotSearchProducts() {
        when(geminiService.analyzeMarketplaceImage(any(byte[].class), eq("image/jpeg")))
                .thenThrow(new IllegalStateException("Gemini unavailable"));

        AppException exception = assertThrows(AppException.class, () -> service.search(
                validJpeg(),
                null,
                null,
                null,
                null,
                null,
                0,
                20));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MARKETPLACE_IMAGE_SEARCH_AI_UNAVAILABLE);
        verifyNoInteractions(entityManager);
    }

    @Test
    void search_withLowConfidence_returnsEmptyProductsAndDoesNotSearchProducts() {
        when(geminiService.analyzeMarketplaceImage(any(byte[].class), eq("image/jpeg")))
                .thenReturn("""
                        {
                          "detectedProduct": "không rõ",
                          "category": null,
                          "keywordsVi": ["không rõ"],
                          "keywordsEn": [],
                          "keywords": ["không rõ"],
                          "visualAttributes": [],
                          "confidence": 0.2,
                          "confidenceLabel": "low",
                          "agricultural": true,
                          "message": "Ảnh chưa đủ rõ."
                        }
                        """);

        MarketplaceImageSearchResponse response = service.search(
                validJpeg(),
                null,
                Boolean.TRUE,
                BigDecimal.ZERO,
                null,
                "price_asc",
                0,
                20);

        assertThat(response.analysis().confidence()).isEqualTo(0.2D);
        assertThat(response.products().getItems()).isEmpty();
        assertThat(response.products().getTotalElements()).isZero();
        verifyNoInteractions(entityManager);
    }

    @Test
    void search_whenGeminiReturnsNonJson_returnsFallbackAnalysisAndDoesNotSearchProducts() {
        when(geminiService.analyzeMarketplaceImage(any(byte[].class), eq("image/jpeg")))
                .thenReturn("I cannot identify the image confidently.");

        MarketplaceImageSearchResponse response = service.search(
                validJpeg(),
                null,
                null,
                null,
                null,
                null,
                0,
                20);

        assertThat(response.analysis().agricultural()).isFalse();
        assertThat(response.analysis().confidence()).isZero();
        assertThat(response.products().getItems()).isEmpty();
        assertThat(response.searchKeywords()).isEmpty();
        verifyNoInteractions(entityManager);
    }

    @Test
    void analyze_whenGeminiReturnsTruncatedJson_recoversDetectedProductAndKeywords() {
        when(geminiService.analyzeMarketplaceImage(any(byte[].class), eq("image/jpeg")))
                .thenReturn("""
                        {
                          "detectedProduct": "Sầu riêng",
                          "category": "Trái cây tươi",
                          "keywordsVi": ["Sầu riêng", "trái cây", "trái cây tươi", "sầu riêng múi"],
                          "keywordsEn": ["Durian", "fresh fruit"
                        """);

        var analysis = service.analyze(validJpeg());

        assertThat(analysis.detectedProduct()).isEqualTo("Sầu riêng");
        assertThat(analysis.category()).isEqualTo("Trái cây tươi");
        assertThat(analysis.keywordsVi()).contains("Sầu riêng", "trái cây tươi");
        assertThat(analysis.keywordsEn()).contains("Durian", "fresh fruit");
        assertThat(analysis.agricultural()).isTrue();
        assertThat(analysis.confidence()).isEqualTo(0.5D);
    }

    private MockMultipartFile validJpeg() {
        return new MockMultipartFile(
                "file",
                "produce.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3, 4});
    }
}
