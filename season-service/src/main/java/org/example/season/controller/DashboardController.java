package org.example.season.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.dto.response.PHIAlertDto;
import org.example.season.service.PHIAlertService;
import org.example.season.service.SeasonWorkspaceAccessService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(\"/api/v1/farmer/dashboard\")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize(\"hasRole('FARMER')\")
public class DashboardController {

    PHIAlertService phiAlertService;
    SeasonWorkspaceAccessService workspaceAccessService;

    @Operation(summary = \"Get PHI Alerts\", description = \"List all active PHI alerts for the current farmer's active seasons\")
    @ApiResponses({
            @ApiResponse(responseCode = \"200\", description = \"Success\"),
            @ApiResponse(responseCode = \"401\", description = \"Unauthorized\"),
            @ApiResponse(responseCode = \"403\", description = \"Forbidden\")
    })
    @GetMapping(\"/phi-alerts\")
    public org.example.season.dto.common.ApiResponse<List<PHIAlertDto>> getPHIAlerts() {
        Long userId = workspaceAccessService.getCurrentUserId();
        return org.example.season.dto.common.ApiResponse.success(phiAlertService.getActivePHIAlerts(userId));
    }
}

