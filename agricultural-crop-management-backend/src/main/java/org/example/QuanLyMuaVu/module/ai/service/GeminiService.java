package org.example.QuanLyMuaVu.module.ai.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-service.url:http://ai-service:8083}")
    private String aiServiceUrl;

    public String chatAsAgriculturalExpert(String userMessage, String cropContext) {
        String url = aiServiceUrl + "/api/v1/internal/ai/agricultural-expert";
        Map<String, String> request = new HashMap<>();
        request.put("userMessage", userMessage);
        request.put("cropContext", cropContext);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.get("result") != null) {
                return (String) response.get("result");
            }
        } catch (Exception ex) {
            log.error("Failed to call ai-service agricultural expert", ex);
        }
        return "Hiện tại tôi không thể kết nối tới dịch vụ tư vấn nông nghiệp. Vui lòng thử lại sau.";
    }

    public String chatAsBuyerProcurementExpert(String userMessage, String buyerContext) {
        String url = aiServiceUrl + "/api/v1/internal/ai/buyer-expert";
        Map<String, String> request = new HashMap<>();
        request.put("userMessage", userMessage);
        request.put("buyerContext", buyerContext);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.get("result") != null) {
                return (String) response.get("result");
            }
        } catch (Exception ex) {
            log.error("Failed to call ai-service buyer expert", ex);
        }
        return "Hiện tại tôi không thể kết nối tới dịch vụ tư vấn mua hàng. Vui lòng thử lại sau.";
    }

    public String analyzeMarketplaceImage(byte[] imageBytes, String mimeType) {
        String url = aiServiceUrl + "/api/v1/internal/ai/analyze-image";
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, String> request = new HashMap<>();
        request.put("imageBytesBase64", base64Image);
        request.put("mimeType", mimeType);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.get("result") != null) {
                return (String) response.get("result");
            }
        } catch (Exception ex) {
            log.error("Failed to call ai-service image analysis", ex);
            throw new IllegalStateException("Gemini image analysis unexpected error", ex);
        }
        throw new IllegalStateException("Gemini image analysis returned empty response");
    }
}
