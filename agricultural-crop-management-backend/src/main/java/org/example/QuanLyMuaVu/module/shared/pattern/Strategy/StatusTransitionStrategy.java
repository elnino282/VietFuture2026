package org.example.QuanLyMuaVu.module.shared.pattern.Strategy;

import java.util.Set;

/**
 * Strategy Pattern: Defines a family of algorithms for status transitions.
 * <p>
 * This interface encapsulates status transition logic, allowing different
 * status types (Season, Task, Incident) to have their own transition rules
 * while sharing a common contract.
 * <p>
 * Benefits:
 * - Eliminates complex if-else chains in services
 * - Easy to add new status types without modifying existing code
 * - Each strategy is independently testable
 *
 * @param <T> The enum type representing the status
 */
public interface StatusTransitionStrategy<T extends Enum<T>> {

    /**
     * Checks if transitioning from currentStatus to targetStatus is valid.
     *
     * @param currentStatus The current status
     * @param targetStatus  The desired target status
     * @return true if the transition is allowed, false otherwise
     */
    boolean canTransition(T currentStatus, T targetStatus);

    /**
     * Returns all statuses that can be transitioned to from the current status.
     *
     * @param currentStatus The current status
     * @return Set of allowed target statuses
     */
    Set<T> getAllowedTransitions(T currentStatus);

    /**
     * Checks if the given status is a terminal (final) status.
     *
     * @param status The status to check
     * @return true if this is a terminal status (no further transitions allowed)
     */
    boolean isTerminalStatus(T status);

    /**
     * Returns the initial/default status for new entities.
     *
     * @return The initial status
     */
    T getInitialStatus();
}
