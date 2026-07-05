package org.example.marketplace.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", fallbackFactory = org.example.marketplace.client.fallback.IdentityClientFallbackFactory.class)
public interface IdentityClient {

    @GetMapping("/api/v1/internal/users/{userId}/display-name")
    String getUserDisplayName(@PathVariable("userId") Long userId);
}
