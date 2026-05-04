package org.example.QuanLyMuaVu.module.farm.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.springframework.stereotype.Service;

/**
 * Farmer Ownership Guard Service
 * 
 * Implements the ownership enforcement foundation as specified:
 * - farms.user_id = currentUserId
 * - plots.farm_id -> farms.user_id = currentUserId
 * - seasons.plot_id -> plots.farm_id -> farms.user_id = currentUserId
 * 
 * This service provides reusable helper methods to verify that a FARMER user
 * owns a specific resource before allowing access or mutation.
 * 
 * Usage pattern:
 * 
 * <pre>
 * {@code
 * // In any farmer service method
 * Farm farm = ownershipService.requireOwnedFarm(farmId);
 * // If we get here, current user owns this farm
 * }
 * </pre>
 * 
 * Error handling:
 * - FARM_NOT_FOUND / PLOT_NOT_FOUND / SEASON_NOT_FOUND if resource doesn't
 * exist
 * - NOT_OWNER (403) if resource exists but belongs to another farmer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerOwnershipService {

    private final CurrentUserService currentUserService;
    private final IdentityQueryPort identityQueryPort;
    private final FarmQueryPort farmQueryPort;
    private final SeasonQueryPort seasonQueryPort;

    // =========================================================================
    // FARM OWNERSHIP
    // =========================================================================

    /**
     * Verify the current user owns the specified farm.
     * 
     * @param farmId the farm ID to check
     * @return the Farm entity if owned by current user
     * @throws AppException with FARM_NOT_FOUND if farm doesn't exist
     * @throws AppException with NOT_OWNER if farm belongs to another user
     */
    public Farm requireOwnedFarm(Integer farmId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return requireOwnedFarm(farmId, currentUserId);
    }

    /**
     * Verify a specific user owns the specified farm.
     * 
     * @param farmId  the farm ID to check
     * @param ownerId the expected owner's user ID
     * @return the Farm entity if owned by specified user
     * @throws AppException with FARM_NOT_FOUND if farm doesn't exist
     * @throws AppException with NOT_OWNER if farm belongs to another user
     */
    public Farm requireOwnedFarm(Integer farmId, Long ownerId) {
        if (farmId == null) {
            throw new AppException(ErrorCode.FARM_NOT_FOUND);
        }

        Farm farm = farmQueryPort.findFarmById(farmId)
                .orElseThrow(() -> new AppException(ErrorCode.FARM_NOT_FOUND));

        if (!farm.getUser().getId().equals(ownerId)) {
            log.warn("Access denied: User {} attempted to access farm {} owned by user {}",
                    ownerId, farmId, farm.getUser().getId());
            throw new AppException(ErrorCode.NOT_OWNER);
        }

        return farm;
    }

    /**
     * Check if current user owns the specified farm (non-throwing version).
     * 
     * @param farmId the farm ID to check
     * @return true if current user owns the farm, false otherwise
     */
    public boolean isOwnedFarm(Integer farmId) {
        try {
            requireOwnedFarm(farmId);
            return true;
        } catch (AppException e) {
            return false;
        }
    }

    /**
     * Get all farms owned by the current user.
     * 
     * @return list of farms owned by current user
     */
    public List<Farm> getOwnedFarms() {
        Long currentUserId = currentUserService.getCurrentUserId();
        if (identityQueryPort.findUserById(currentUserId).isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return farmQueryPort.findFarmsByOwnerId(currentUserId);
    }

    // =========================================================================
    // PLOT OWNERSHIP (via farm)
    // =========================================================================

    /**
     * Verify the current user owns the specified plot (via its farm).
     * 
     * @param plotId the plot ID to check
     * @return the Plot entity if owned by current user
     * @throws AppException with PLOT_NOT_FOUND if plot doesn't exist
     * @throws AppException with NOT_OWNER if plot's farm belongs to another user
     */
    public Plot requireOwnedPlot(Integer plotId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return requireOwnedPlot(plotId, currentUserId);
    }

    /**
     * Verify a specific user owns the specified plot (via its farm).
     * 
     * @param plotId  the plot ID to check
     * @param ownerId the expected owner's user ID
     * @return the Plot entity if owned by specified user
     * @throws AppException with PLOT_NOT_FOUND if plot doesn't exist
     * @throws AppException with NOT_OWNER if plot's farm belongs to another user
     */
    public Plot requireOwnedPlot(Integer plotId, Long ownerId) {
        if (plotId == null) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }

        Plot plot = farmQueryPort.findPlotById(plotId)
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));

        // Check via farm ownership
        Farm farm = plot.getFarm();
        if (farm == null || !farm.getUser().getId().equals(ownerId)) {
            log.warn("Access denied: User {} attempted to access plot {} via farm owned by user {}",
                    ownerId, plotId, farm != null ? farm.getUser().getId() : "null");
            throw new AppException(ErrorCode.NOT_OWNER);
        }

        return plot;
    }

    /**
     * Check if current user owns the specified plot (non-throwing version).
     * 
     * @param plotId the plot ID to check
     * @return true if current user owns the plot, false otherwise
     */
    public boolean isOwnedPlot(Integer plotId) {
        try {
            requireOwnedPlot(plotId);
            return true;
        } catch (AppException e) {
            return false;
        }
    }

    /**
     * Get all plots for farms owned by the current user.
     * 
     * @return list of plots for current user's farms
     */
    public List<Plot> getOwnedPlots() {
        Long currentUserId = currentUserService.getCurrentUserId();
        if (identityQueryPort.findUserById(currentUserId).isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return farmQueryPort.findPlotsByOwnerId(currentUserId);
    }

    // =========================================================================
    // SEASON OWNERSHIP (via plot -> farm)
    // =========================================================================

    /**
     * Verify the current user owns the specified season (via plot -> farm).
     * 
     * @param seasonId the season ID to check
     * @return the org.example.QuanLyMuaVu.module.season.entity.Season entity if owned by current user
     * @throws AppException with SEASON_NOT_FOUND if season doesn't exist
     * @throws AppException with NOT_OWNER if season's farm belongs to another user
     */
    public org.example.QuanLyMuaVu.module.season.entity.Season requireOwnedSeason(Integer seasonId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return requireOwnedSeason(seasonId, currentUserId);
    }

    /**
     * Verify a specific user owns the specified season (via plot -> farm).
     * 
     * @param seasonId the season ID to check
     * @param ownerId  the expected owner's user ID
     * @return the org.example.QuanLyMuaVu.module.season.entity.Season entity if owned by specified user
     * @throws AppException with SEASON_NOT_FOUND if season doesn't exist
     * @throws AppException with NOT_OWNER if season's farm belongs to another user
     */
    public org.example.QuanLyMuaVu.module.season.entity.Season requireOwnedSeason(Integer seasonId, Long ownerId) {
        if (seasonId == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }

        org.example.QuanLyMuaVu.module.season.entity.Season season = seasonQueryPort.findSeasonById(seasonId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_NOT_FOUND));

        // Check via plot -> farm ownership chain
        Plot plot = season.getPlot();
        if (plot == null) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }

        Farm farm = plot.getFarm();
        if (farm == null || !farm.getUser().getId().equals(ownerId)) {
            log.warn("Access denied: User {} attempted to access season {} via farm owned by user {}",
                    ownerId, seasonId, farm != null ? farm.getUser().getId() : "null");
            throw new AppException(ErrorCode.NOT_OWNER);
        }

        return season;
    }

    /**
     * Check if current user owns the specified season (non-throwing version).
     * 
     * @param seasonId the season ID to check
     * @return true if current user owns the season, false otherwise
     */
    public boolean isOwnedSeason(Integer seasonId) {
        try {
            requireOwnedSeason(seasonId);
            return true;
        } catch (AppException e) {
            return false;
        }
    }

    /**
     * Get all seasons for farms owned by the current user.
     * 
     * @return list of seasons for current user's farms
     */
    public List<org.example.QuanLyMuaVu.module.season.entity.Season> getOwnedSeasons() {
        // Get all farm IDs owned by current user
        List<Integer> farmIds = getOwnedFarms().stream()
                .map(Farm::getId)
                .toList();

        if (farmIds.isEmpty()) {
            return List.of();
        }

        return seasonQueryPort.findAllSeasonsByFarmIds(farmIds);
    }
}
