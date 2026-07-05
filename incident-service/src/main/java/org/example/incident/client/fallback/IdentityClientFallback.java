package org.example.incident.client.fallback;

import org.example.incident.client.IdentityFeignClient;
import org.example.incident.service.ExternalServiceClient.UserInternalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdentityClientFallback implements IdentityFeignClient {
    private static final Logger log = LoggerFactory.getLogger(IdentityClientFallback.class);

    @Override
    public UserInternalDto getUser(Long userId) {
        log.error("Fallback triggered for IdentityFeignClient.getUser with userId={}. Identity service might be down.", userId);
        return null;
    }
}
