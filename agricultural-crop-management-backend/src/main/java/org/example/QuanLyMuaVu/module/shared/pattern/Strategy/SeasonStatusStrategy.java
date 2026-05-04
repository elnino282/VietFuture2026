package org.example.QuanLyMuaVu.module.shared.pattern.Strategy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.example.QuanLyMuaVu.Enums.SeasonStatus;
import org.springframework.stereotype.Component;

/**
 * Strategy Pattern Implementation: Season Status Transitions.
 * <p>
 * Encapsulates the state machine for Season lifecycle:
 * 
 * <pre>
 *     PLANNED ──→ ACTIVE ──→ COMPLETED
 *        │          │            │
 *        └──────────┴────────────┴──→ CANCELLED
 * </pre>
 * <p>
 * Business Rules:
 * - PLANNED: Initial state for newly created seasons
 * - ACTIVE: Season is in progress (farming activities ongoing)
 * - COMPLETED: Season successfully finished (harvest done)
 * - CANCELLED: Season terminated early (any state can transition here)
 */
@Component
public class SeasonStatusStrategy implements StatusTransitionStrategy<SeasonStatus> {

    private static final Map<SeasonStatus, Set<SeasonStatus>> TRANSITIONS = new EnumMap<>(SeasonStatus.class);

    static {
        // PLANNED can go to ACTIVE or CANCELLED
        TRANSITIONS.put(SeasonStatus.PLANNED, EnumSet.of(SeasonStatus.ACTIVE, SeasonStatus.CANCELLED));

        // ACTIVE can go to COMPLETED, CANCELLED, or ARCHIVED
        TRANSITIONS.put(SeasonStatus.ACTIVE, EnumSet.of(
                SeasonStatus.COMPLETED,
                SeasonStatus.CANCELLED,
                SeasonStatus.ARCHIVED));

        // COMPLETED can be archived
        TRANSITIONS.put(SeasonStatus.COMPLETED, EnumSet.of(SeasonStatus.ARCHIVED));

        // CANCELLED can be archived
        TRANSITIONS.put(SeasonStatus.CANCELLED, EnumSet.of(SeasonStatus.ARCHIVED));

        // ARCHIVED is terminal - no further transitions
        TRANSITIONS.put(SeasonStatus.ARCHIVED, EnumSet.noneOf(SeasonStatus.class));
    }

    @Override
    public boolean canTransition(SeasonStatus currentStatus, SeasonStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        if (currentStatus == targetStatus) {
            return true; // No-op transitions are allowed
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
