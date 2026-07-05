package org.example.finance.client;

import lombok.extern.slf4j.Slf4j;
import org.example.finance.service.ExternalServiceClient.UserInternalDto;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IdentityServiceClientFallback implements IdentityServiceClient {

    @Override
    public UserInternalDto getUser(Long userId) {
        log.error("Fallback triggered: Failed to fetch user {} from identity-service", userId);
        return null;
    }
}
