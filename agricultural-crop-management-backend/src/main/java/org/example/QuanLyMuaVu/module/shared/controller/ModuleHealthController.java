package org.example.QuanLyMuaVu.module.shared.controller;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class ModuleHealthController {

    private static final List<String> SUPPORTED_MODULES = List.of(
            "ai",
            "cropcatalog",
            "identity",
            "farm",
            "season",
            "financial",
            "inventory",
            "incident",
            "sustainability",
            "admin");

    @GetMapping("/health/modules")
    public ApiResponse<List<ModuleHealthStatus>> moduleHealthOverview() {
        Instant now = Instant.now();
        List<ModuleHealthStatus> result = SUPPORTED_MODULES.stream()
                .map(module -> new ModuleHealthStatus(module, "UP", now.toString()))
                .toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/health/modules/{moduleName}")
    public ResponseEntity<ApiResponse<ModuleHealthStatus>> moduleHealth(@PathVariable String moduleName) {
        String normalized = moduleName.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_MODULES.contains(normalized)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(
                            HttpStatus.NOT_FOUND,
                            "MODULE_NOT_FOUND",
                            "Module '" + moduleName + "' is not configured for health checks"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                new ModuleHealthStatus(normalized, "UP", Instant.now().toString())));
    }

    public record ModuleHealthStatus(String module, String status, String checkedAt) {
    }
}
