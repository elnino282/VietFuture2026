package org.example.finance.client;

import org.example.finance.service.ExternalServiceClient.UserInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", url = "${app.identity-service-url:http://localhost:8081}", fallback = IdentityServiceClientFallback.class)
public interface IdentityServiceClient {

    @GetMapping("/api/v1/internal/users/{userId}")
    UserInternalDto getUser(@PathVariable("userId") Long userId);
}
