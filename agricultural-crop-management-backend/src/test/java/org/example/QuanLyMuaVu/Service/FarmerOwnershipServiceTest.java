package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.farm.service.FarmerOwnershipService;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.example.QuanLyMuaVu.module.shared.security.CurrentUserService;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FarmerOwnershipService.
 * 
 * Tests ownership enforcement for:
 * - Farm ownership (farms.user_id = currentUserId)
 * - Plot ownership (via farm)
 * - Season ownership (via plot -> farm)
 */
@ExtendWith(MockitoExtension.class)
public class FarmerOwnershipServiceTest {

        @Mock
        private CurrentUserService currentUserService;

        @Mock
        private IdentityQueryPort identityQueryPort;

        @Mock
        private FarmQueryPort farmQueryPort;

        @Mock
        private SeasonQueryPort seasonQueryPort;

        @InjectMocks
        private FarmerOwnershipService ownershipService;

        private User owner;
        private User otherUser;
        private Farm ownedFarm;
        private Farm foreignFarm;
        private Plot ownedPlot;
        private Plot foreignPlot;
        private Season ownedSeason;
        private Season foreignSeason;
        private Crop crop;

        @BeforeEach
        void setUp() {
                // Create users
                owner = User.builder()
                                .id(1L)
                                .username("owner")
                                .email("owner@test.local")
                                .status(UserStatus.ACTIVE)
                                .build();

                otherUser = User.builder()
                                .id(2L)
                                .username("other")
                                .email("other@test.local")
                                .status(UserStatus.ACTIVE)
                                .build();

                // Create farms
                ownedFarm = Farm.builder()
                                .id(1)
                                .name("Owned Farm")
                                .user(owner)
                                .area(BigDecimal.valueOf(10.0))
                                .active(true)
                                .build();

                foreignFarm = Farm.builder()
                                .id(2)
                                .name("Foreign Farm")
                                .user(otherUser)
                                .area(BigDecimal.valueOf(15.0))
                                .active(true)
                                .build();

                // Create plots
                ownedPlot = Plot.builder()
                                .id(1)
                                .plotName("Owned Plot")
                                .farm(ownedFarm)
                                .user(owner)
                                .area(BigDecimal.valueOf(5.0))
                                .build();

                foreignPlot = Plot.builder()
                                .id(2)
                                .plotName("Foreign Plot")
                                .farm(foreignFarm)
                                .user(otherUser)
                                .area(BigDecimal.valueOf(7.0))
                                .build();

                // Create crop
                crop = Crop.builder()
                                .id(1)
                                .cropName("Test Crop")
                                .build();

                // Create seasons
                ownedSeason = Season.builder()
                                .id(1)
                                .seasonName("Owned Season")
                                .plot(ownedPlot)
                                .crop(crop)
                                .startDate(LocalDate.now())
                                .status(SeasonStatus.ACTIVE)
                                .initialPlantCount(100)
                                .build();

                foreignSeason = Season.builder()
                                .id(2)
                                .seasonName("Foreign Season")
                                .plot(foreignPlot)
                                .crop(crop)
                                .startDate(LocalDate.now())
                                .status(SeasonStatus.ACTIVE)
                                .initialPlantCount(200)
                                .build();
        }

        // =========================================================================
        // FARM OWNERSHIP TESTS
        // =========================================================================

        @Test
        @DisplayName("requireOwnedFarm succeeds for owned farm")
        void testRequireOwnedFarm_Success() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(farmQueryPort.findFarmById(ownedFarm.getId())).thenReturn(Optional.of(ownedFarm));

                Farm result = ownershipService.requireOwnedFarm(ownedFarm.getId());

                assertNotNull(result);
                assertEquals(ownedFarm.getId(), result.getId());
                assertEquals(owner.getId(), result.getUser().getId());
        }

        @Test
        @DisplayName("requireOwnedFarm throws NOT_OWNER for foreign farm")
        void testRequireOwnedFarm_ForeignFarm() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(farmQueryPort.findFarmById(foreignFarm.getId())).thenReturn(Optional.of(foreignFarm));

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedFarm(foreignFarm.getId()));

                assertEquals(ErrorCode.NOT_OWNER, exception.getErrorCode());
        }

        @Test
        @DisplayName("requireOwnedFarm throws FARM_NOT_FOUND for non-existent farm")
        void testRequireOwnedFarm_NotFound() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(farmQueryPort.findFarmById(999)).thenReturn(Optional.empty());

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedFarm(999));

                assertEquals(ErrorCode.FARM_NOT_FOUND, exception.getErrorCode());
        }

        // =========================================================================
        // PLOT OWNERSHIP TESTS
        // =========================================================================

        @Test
        @DisplayName("requireOwnedPlot succeeds for owned plot")
        void testRequireOwnedPlot_Success() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(farmQueryPort.findPlotById(ownedPlot.getId())).thenReturn(Optional.of(ownedPlot));

                Plot result = ownershipService.requireOwnedPlot(ownedPlot.getId());

                assertNotNull(result);
                assertEquals(ownedPlot.getId(), result.getId());
                assertEquals(owner.getId(), result.getFarm().getUser().getId());
        }

        @Test
        @DisplayName("requireOwnedPlot throws NOT_OWNER for foreign plot")
        void testRequireOwnedPlot_ForeignPlot() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(farmQueryPort.findPlotById(foreignPlot.getId())).thenReturn(Optional.of(foreignPlot));

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedPlot(foreignPlot.getId()));

                assertEquals(ErrorCode.NOT_OWNER, exception.getErrorCode());
        }

        @Test
        @DisplayName("requireOwnedPlot throws PLOT_NOT_FOUND for non-existent plot")
        void testRequireOwnedPlot_NotFound() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(farmQueryPort.findPlotById(999)).thenReturn(Optional.empty());

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedPlot(999));

                assertEquals(ErrorCode.PLOT_NOT_FOUND, exception.getErrorCode());
        }

        // =========================================================================
        // SEASON OWNERSHIP TESTS
        // =========================================================================

        @Test
        @DisplayName("requireOwnedSeason succeeds for owned season")
        void testRequireOwnedSeason_Success() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(seasonQueryPort.findSeasonById(ownedSeason.getId())).thenReturn(Optional.of(ownedSeason));

                Season result = ownershipService.requireOwnedSeason(ownedSeason.getId());

                assertNotNull(result);
                assertEquals(ownedSeason.getId(), result.getId());
                assertEquals(owner.getId(), result.getPlot().getFarm().getUser().getId());
        }

        @Test
        @DisplayName("requireOwnedSeason throws NOT_OWNER for foreign season")
        void testRequireOwnedSeason_ForeignSeason() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(seasonQueryPort.findSeasonById(foreignSeason.getId())).thenReturn(Optional.of(foreignSeason));

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedSeason(foreignSeason.getId()));

                assertEquals(ErrorCode.NOT_OWNER, exception.getErrorCode());
        }

        @Test
        @DisplayName("requireOwnedSeason throws SEASON_NOT_FOUND for non-existent season")
        void testRequireOwnedSeason_NotFound() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());
                when(seasonQueryPort.findSeasonById(999)).thenReturn(Optional.empty());

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedSeason(999));

                assertEquals(ErrorCode.SEASON_NOT_FOUND, exception.getErrorCode());
        }

        // =========================================================================
        // NULL ID TESTS
        // =========================================================================

        @Test
        @DisplayName("requireOwnedFarm throws FARM_NOT_FOUND for null farmId")
        void testRequireOwnedFarm_NullId() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedFarm(null));

                assertEquals(ErrorCode.FARM_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("requireOwnedPlot throws PLOT_NOT_FOUND for null plotId")
        void testRequireOwnedPlot_NullId() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedPlot(null));

                assertEquals(ErrorCode.PLOT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("requireOwnedSeason throws SEASON_NOT_FOUND for null seasonId")
        void testRequireOwnedSeason_NullId() {
                when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

                AppException exception = assertThrows(AppException.class,
                                () -> ownershipService.requireOwnedSeason(null));

                assertEquals(ErrorCode.SEASON_NOT_FOUND, exception.getErrorCode());
        }
}
