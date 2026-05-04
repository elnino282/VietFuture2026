package org.example.QuanLyMuaVu.module.season.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.season.dto.request.CreateHarvestDetailRequest;
import org.example.QuanLyMuaVu.module.season.dto.request.UpdateHarvestDetailRequest;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestStockContextResponse;
import org.example.QuanLyMuaVu.module.season.dto.response.HarvestSummaryResponse;
import org.example.QuanLyMuaVu.module.season.service.SeasonHarvestService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for managing harvest batches of a season (business flow [5])
 * for farms owned by or shared with the current farmer.
 */

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('FARMER')")
public class SeasonHarvestController {

        SeasonHarvestService seasonHarvestService;

        @Operation(summary = "List all farmer harvests", description = "List all harvests for current farmer, optionally filtered by season")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
        })
        @GetMapping("/harvests")
        public ApiResponse<PageResponse<HarvestResponse>> listAllHarvests(
                        @Parameter(description = "Season ID (optional, if absent returns all farmer's harvests)") @RequestParam(value = "seasonId", required = false) Integer seasonId,
                        @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
                return ApiResponse.success(seasonHarvestService.listAllFarmerHarvests(seasonId, from, to, page, size));
        }

        @Operation(summary = "Get harvest summary", description = "Get harvest KPI summary for current farmer, optionally filtered by season")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
        })
        @GetMapping("/harvests/summary")
        public ApiResponse<HarvestSummaryResponse> getSummary(
                        @Parameter(description = "Season ID (optional, if absent returns summary for all farmer's seasons)") @RequestParam(value = "seasonId", required = false) Integer seasonId) {
                return ApiResponse.success(seasonHarvestService.getSummary(seasonId));
        }

        @Operation(summary = "List harvests of a season", description = "List harvest batches for a season")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @GetMapping("/seasons/{seasonId}/harvests")
        public ApiResponse<PageResponse<HarvestResponse>> listHarvests(
                        @PathVariable Integer seasonId,
                        @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {
                return ApiResponse.success(seasonHarvestService.listHarvestsForSeason(seasonId, from, to, page, size));
        }

        @Operation(summary = "Get harvest stock context", description = "Preview existing warehouse stock for selected warehouse/product/lot before recording harvest")
        @GetMapping("/seasons/{seasonId}/harvests/stock-context")
        public ApiResponse<HarvestStockContextResponse> getHarvestStockContext(
                        @PathVariable Integer seasonId,
                        @RequestParam("warehouseId") Integer warehouseId,
                        @RequestParam("productName") String productName,
                        @RequestParam("lotCode") String lotCode) {
                return ApiResponse.success(
                                seasonHarvestService.getStockContext(seasonId, warehouseId, productName, lotCode));
        }

        @Operation(summary = "Create harvest batch", description = "Create a new harvest batch for a season")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Season not found")
        })
        @PostMapping("/seasons/{seasonId}/harvests")
        public ApiResponse<HarvestResponse> createHarvest(
                        @PathVariable Integer seasonId,
                        @Valid @RequestBody CreateHarvestDetailRequest request) {
                return ApiResponse.success(seasonHarvestService.createHarvest(seasonId, request));
        }

        @Operation(summary = "Get harvest detail", description = "Get harvest detail if it belongs to a season of current farmer")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Harvest not found")
        })
        @GetMapping("/harvests/{id}")
        public ApiResponse<HarvestResponse> getHarvest(@PathVariable Integer id) {
                return ApiResponse.success(seasonHarvestService.getHarvest(id));
        }

        @Operation(summary = "Update harvest", description = "Update harvest information while constraints are respected")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Harvest not found")
        })
        @PutMapping("/harvests/{id}")
        public ApiResponse<HarvestResponse> updateHarvest(
                        @PathVariable Integer id,
                        @Valid @RequestBody UpdateHarvestDetailRequest request) {
                return ApiResponse.success(seasonHarvestService.updateHarvest(id, request));
        }

        @Operation(summary = "Delete harvest", description = "Delete a harvest batch if it has no linked orders or quality results")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Harvest not found")
        })
        @DeleteMapping("/harvests/{id}")
        public ApiResponse<Void> deleteHarvest(@PathVariable Integer id) {
                seasonHarvestService.deleteHarvest(id);
                return ApiResponse.success(null);
        }
}
