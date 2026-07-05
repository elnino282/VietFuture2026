package org.example.incident.client;

import org.example.incident.service.ExternalServiceClient.UserInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.example.incident.client.fallback.IdentityClientFallback;

@FeignClient(name = "identity-service", url = "${app.identity-service-url:http://localhost:8081}", fallback = IdentityClientFallback.class)
public interface IdentityFeignClient {
    @GetMapping("/api/v1/internal/users/{userId}")
    UserInternalDto getUser(@PathVariable("userId") Long userId);
}
