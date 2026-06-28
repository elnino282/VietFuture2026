package org.example.marketplace.client.impl;

import org.example.marketplace.client.IdentityClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class IdentityClientImpl implements IdentityClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityClientImpl.class);

    @Value("${external-services.identity-service-url:http://identity-service:8081}")
    private String identityServiceUrl;

    private WebClient getWebClient() {
        return WebClient.builder()
                .baseUrl(identityServiceUrl)
                .build();
    }

    @Override
    public String getUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return getWebClient().get()
                    .uri("/api/v1/internal/users/{userId}/display-name", userId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch display name for userId={}: {}. Returning null.", userId, e.getMessage());
            return null;
        }
    }
}
