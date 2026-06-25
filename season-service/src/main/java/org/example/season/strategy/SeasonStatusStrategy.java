package org.example.season.strategy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.example.season.enums.SeasonStatus;
import org.springframework.stereotype.Component;

@Component
public class SeasonStatusStrategy implements StatusTransitionStrategy<SeasonStatus> {

    private static final Map<SeasonStatus, Set<SeasonStatus>> TRANSITIONS = new EnumMap<>(SeasonStatus.class);

    static {
        TRANSITIONS.put(SeasonStatus.PLANNED, EnumSet.of(SeasonStatus.ACTIVE, SeasonStatus.CANCELLED));
        TRANSITIONS.put(SeasonStatus.ACTIVE, EnumSet.of(
                SeasonStatus.COMPLETED,
                SeasonStatus.CANCELLED,
                SeasonStatus.ARCHIVED));
        TRANSITIONS.put(SeasonStatus.COMPLETED, EnumSet.of(SeasonStatus.ARCHIVED));
        TRANSITIONS.put(SeasonStatus.CANCELLED, EnumSet.of(SeasonStatus.ARCHIVED));
        TRANSITIONS.put(SeasonStatus.ARCHIVED, EnumSet.noneOf(SeasonStatus.class));
    }

    @Override
    public boolean canTransition(SeasonStatus currentStatus, SeasonStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return true;
        }
        Set<SeasonStatus> allowed = TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    @Override
    public Set<SeasonStatus> getAllowedTransitions(SeasonStatus currentStatus) {
        if (currentStatus == null) {
            return EnumSet.noneOf(SeasonStatus.class);
        }
        Set<SeasonStatus> allowed = TRANSITIONS.get(currentStatus);
        return allowed != null ? EnumSet.copyOf(allowed) : EnumSet.noneOf(SeasonStatus.class);
    }

    @Override
    public boolean isTerminalStatus(SeasonStatus status) {
        return status == SeasonStatus.ARCHIVED || status == SeasonStatus.CANCELLED;
    }

    @Override
    public SeasonStatus getInitialStatus() {
        return SeasonStatus.PLANNED;
    }
}
