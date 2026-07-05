package org.example.marketplace.client.fallback;

import org.example.marketplace.client.IdentityClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IdentityClientFallback implements IdentityClient {

    @Override
    public String getUserDisplayName(Long userId) {
        log.error("Fallback triggered: Failed to fetch display name for userId={}", userId);
        return null;
    }
}
