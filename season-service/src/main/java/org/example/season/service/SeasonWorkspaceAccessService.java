package org.example.season.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.season.config.CurrentUserService;
import org.example.season.exception.AppException;
import org.example.season.exception.ErrorCode;
import org.example.season.entity.Season;
import org.example.season.entity.SeasonEmployee;
import org.example.season.repository.SeasonEmployeeRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeasonWorkspaceAccessService {

    public static final String ACTOR_TYPE_FARMER = "FARMER";
    public static final String ACTOR_TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String ACTOR_TYPE_UNKNOWN = "UNKNOWN";

    CurrentUserService currentUserService;
    ExternalServiceClient externalServiceClient;
    SeasonEmployeeRepository seasonEmployeeRepository;

    public Long getCurrentUserId() {
        return currentUserService.getCurrentUserId();
    }

    public ExternalServiceClient.UserInternalDto getCurrentUser() {
        Long userId = getCurrentUserId();
        return userId != null ? externalServiceClient.getUser(userId) : null;
    }


    public void assertCurrentUserCanAccessSeason(Season season) {
        if (isCurrentUserSeasonOwner(season)) {
            return;
        }
        requireActiveEmployeeAssignment(season);
    }

    public void assertCurrentUserCanAccessPlot(Integer plotId) {
        if (plotId == null) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(plotId);
        if (plot == null) {
            throw new AppException(ErrorCode.PLOT_NOT_FOUND);
        }
        Long currentUserId = getCurrentUserId();
        if (plot.getOwnerUserId() != null && plot.getOwnerUserId().equals(currentUserId)) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }

    public SeasonEmployee requireActiveEmployeeAssignment(Season season) {
        if (season == null || season.getId() == null) {
            throw new AppException(ErrorCode.SEASON_NOT_FOUND);
        }
        Long currentUserId = getCurrentUserId();
        SeasonEmployee assignment = seasonEmployeeRepository
                .findBySeasonIdAndEmployeeUserId(season.getId(), currentUserId)
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
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId();
        } catch (RuntimeException ex) {
            return false;
        }
        Long ownerId = resolveSeasonOwnerId(season);
        return ownerId != null && currentUserId != null && ownerId.equals(currentUserId);
    }

    public Long resolveSeasonOwnerId(Season season) {
        if (season == null || season.getPlotId() == null) {
            return null;
        }
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
        return plot != null ? plot.getOwnerUserId() : null;
    }

    public Integer resolveSeasonFarmId(Season season) {
        if (season == null || season.getPlotId() == null) {
            return null;
        }
        ExternalServiceClient.PlotInternalDto plot = externalServiceClient.getPlot(season.getPlotId());
        return plot != null ? plot.getFarmId() : null;
    }

    public String resolveActorType(Season season, ExternalServiceClient.UserInternalDto actor) {
        if (actor == null || actor.getId() == null) {
            return ACTOR_TYPE_UNKNOWN;
        }
        Long ownerId = resolveSeasonOwnerId(season);
        if (ownerId != null && ownerId.equals(actor.getId())) {
            return ACTOR_TYPE_FARMER;
        }
        if (season != null
                && season.getId() != null
                && seasonEmployeeRepository.existsBySeasonIdAndEmployeeUserId(season.getId(), actor.getId())) {
            return ACTOR_TYPE_EMPLOYEE;
        }
        return ACTOR_TYPE_UNKNOWN;
    }

    public String resolveDisplayName(ExternalServiceClient.UserInternalDto user) {
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

    public java.util.List<Integer> getAccessibleFarmIdsForCurrentUser() {
        Long currentUserId = getCurrentUserId();
        return currentUserId != null ? externalServiceClient.getAccessibleFarmIdsForUser(currentUserId) : java.util.List.of();
    }
}

