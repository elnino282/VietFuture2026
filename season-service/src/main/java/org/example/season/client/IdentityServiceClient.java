package org.example.season.client;

import org.example.season.service.ExternalServiceClient.UserInternalDto;
import org.example.season.service.ExternalServiceClient.EmployeePageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "identity-service", url = "${app.identity-service-url}", fallback = IdentityServiceClientFallback.class)
public interface IdentityServiceClient {

    @GetMapping("/api/v1/internal/users/{userId}")
    UserInternalDto getUser(@PathVariable("userId") Long userId);

    @GetMapping("/api/v1/internal/users/employees")
    EmployeePageResponse searchEmployees(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam(value = "keyword", required = false) String keyword);

    @GetMapping("/api/v1/internal/users/employees/{userId}/validate")
    Boolean validateEmployee(@PathVariable("userId") Long userId);
}
