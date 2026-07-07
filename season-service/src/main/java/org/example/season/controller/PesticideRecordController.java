package org.example.season.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.dto.request.CreatePesticideRecordRequest;
import org.example.season.dto.response.PesticideRecordResponse;
import org.example.season.service.PesticideRecordService;
import org.example.season.service.SeasonWorkspaceAccessService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seasons/{seasonId}/phi")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class PesticideRecordController {

    PesticideRecordService pesticideRecordService;
    SeasonWorkspaceAccessService workspaceAccessService;

    @Operation(summary = "Create pesticide record", description = "Create a pesticide record for a season with safety PHI lookup")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Season not found")
    })
    @PostMapping("/records")
    public org.example.season.dto.common.ApiResponse<PesticideRecordResponse> createRecord(
            @PathVariable Integer seasonId,
            @Valid @RequestBody CreatePesticideRecordRequest request) {
        Long userId = workspaceAccessService.getCurrentUserId();
        return org.example.season.dto.common.ApiResponse.success(pesticideRecordService.create(seasonId, request, userId));
    }

    @Operation(summary = "Get active PHI safety records", description = "List all pesticide records currently within their pre-harvest quarantine interval")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Season not found")
    })
    @GetMapping("/active")
    public org.example.season.dto.common.ApiResponse<List<PesticideRecordResponse>> getActivePHI(@PathVariable Integer seasonId) {
        return org.example.season.dto.common.ApiResponse.success(pesticideRecordService.getActivePHI(seasonId));
    }

    @Operation(summary = "Get all pesticide records", description = "Get all pesticide application records for a season")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Season not found")
    })
    @GetMapping("/records")
    public org.example.season.dto.common.ApiResponse<List<PesticideRecordResponse>> getRecords(@PathVariable Integer seasonId) {
        return org.example.season.dto.common.ApiResponse.success(pesticideRecordService.getBySeason(seasonId));
    }
}
