package org.example.QuanLyMuaVu.module.season.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.farm.entity.Farm;
import org.example.QuanLyMuaVu.module.farm.entity.Plot;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.season.entity.SeasonEmployee;
import org.example.QuanLyMuaVu.module.season.repository.SeasonEmployeeRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeasonWorkspaceAccessService {

    public static final String ACTOR_TYPE_FARMER = "FARMER";
    public static final String ACTOR_TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String ACTOR_TYPE_UNKNOWN = "UNKNOWN";

    FarmAccessPort farmAccessService;
    SeasonEmployeeRepository seasonEmployeeRepository;

    public User getCurrentUser() {
        return farmAccessService.getCurrentUser();
    }

    public Long getCurrentUserId() {
        return farmAccessService.getCurrentUserId();
    }

    public void assertCurrentUserCanAccessSeason(Season season) {
        if (isCurrentUserSeasonOwner(season)) {
            return;
        }
        requireActiveEmployeeAssignment(season);
    }

    public SeasonEmployee requireActiveEmployeeAssignment(Season season) {
        if (season == null || season.getId() == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        Long currentUserId = getCurrentUserId();
        SeasonEmployee assignment = seasonEmployeeRepository
                .findBySeason_IdAndEmployee_Id(season.getId(), currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND));
        if (!Boolean.TRUE.equals(assignment.getActive())) {
            throw new AppException(ErrorCode.SEASON_EMPLOYEE_NOT_FOUND);
        }
        return assignment;
    }

    public void assertCurrentUserCanManageRecord(Season season, Long actorUserId) {
        if (isCurrentUserSeasonOwner(season)) {
            return;
        }
        requireActiveEmployeeAssignment(season);
        Long currentUserId = getCurrentUserId();
        if (actorUserId != null && actorUserId.equals(currentUserId)) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }

    public boolean canCurrentUserManageRecord(Season season, Long actorUserId) {
        try {
            if (isCurrentUserSeasonOwner(season)) {
                return true;
            }
            requireActiveEmployeeAssignment(season);
            Long currentUserId = getCurrentUserId();
            return actorUserId != null && actorUserId.equals(currentUserId);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean isCurrentUserSeasonOwner(Season season) {
        User currentUser;
        try {
            currentUser = getCurrentUser();
        } catch (RuntimeException ex) {
            return false;
        }
        Long ownerId = resolveSeasonOwnerId(season);
        return ownerId != null && currentUser.getId() != null && ownerId.equals(currentUser.getId());
    }

    public Long resolveSeasonOwnerId(Season season) {
        Plot plot = season != null ? season.getPlot() : null;
        if (plot == null) {
            return null;
        }
        Farm farm = plot.getFarm();
        if (farm != null && farm.getUser() != null) {
            return farm.getUser().getId();
        }
        return plot.getUser() != null ? plot.getUser().getId() : null;
    }

    public Integer resolveSeasonFarmId(Season season) {
        Plot plot = season != null ? season.getPlot() : null;
        Farm farm = plot != null ? plot.getFarm() : null;
        return farm != null ? farm.getId() : null;
    }

    public String resolveActorType(Season season, User actor) {
        if (actor == null || actor.getId() == null) {
            return ACTOR_TYPE_UNKNOWN;
        }
        Long ownerId = resolveSeasonOwnerId(season);
        if (ownerId != null && ownerId.equals(actor.getId())) {
            return ACTOR_TYPE_FARMER;
        }
        if (season != null
                && season.getId() != null
                && seasonEmployeeRepository.existsBySeason_IdAndEmployee_Id(season.getId(), actor.getId())) {
            return ACTOR_TYPE_EMPLOYEE;
        }
        return ACTOR_TYPE_UNKNOWN;
    }

    public String resolveDisplayName(User user) {
        if (user == null) {
            return null;
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }
}
