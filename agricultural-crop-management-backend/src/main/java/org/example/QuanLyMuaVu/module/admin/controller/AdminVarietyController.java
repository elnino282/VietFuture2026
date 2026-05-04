package org.example.QuanLyMuaVu.module.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.request.VarietyRequest;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.VarietyResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.service.VarietyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST endpoints for system-wide variety management.
 * Provides full CRUD operations for crop varieties.
 */
@RestController
@RequestMapping("/api/v1/admin/varieties")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Varieties", description = "Admin endpoints for system-wide variety management")
public class AdminVarietyController {

    VarietyService varietyService;

    @Operation(summary = "List varieties (Admin)", description = "Get list of all varieties, optionally filtered by crop")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<List<VarietyResponse>> listVarieties(
            @Parameter(description = "Filter by crop ID") @RequestParam(value = "cropId", required = false) Integer cropId) {
        if (cropId != null) {
            return ApiResponse.success(varietyService.getByCropId(cropId));
        }
        return ApiResponse.success(varietyService.getAll());
    }

    @Operation(summary = "Get variety detail (Admin)", description = "Get detailed information about a specific variety")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @GetMapping("/{id}")
    public ApiResponse<VarietyResponse> getVariety(@PathVariable Integer id) {
        return ApiResponse.success(varietyService.get(id));
    }

    @Operation(summary = "Create variety (Admin)", description = "Create a new variety for a crop")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    public ApiResponse<VarietyResponse> createVariety(@Valid @RequestBody VarietyRequest request) {
        return ApiResponse.success(varietyService.create(request));
    }

    @Operation(summary = "Update variety (Admin)", description = "Update an existing variety")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @PutMapping("/{id}")
    public ApiResponse<VarietyResponse> updateVariety(
            @PathVariable Integer id,
            @Valid @RequestBody VarietyRequest request) {
        return ApiResponse.success(varietyService.update(id, request));
    }

    @Operation(summary = "Delete variety (Admin)", description = "Delete an existing variety. Fails if referenced in seasons.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Variety is referenced in seasons")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteVariety(@PathVariable Integer id) {
        varietyService.delete(id);
        return ApiResponse.success(null);
    }
}

