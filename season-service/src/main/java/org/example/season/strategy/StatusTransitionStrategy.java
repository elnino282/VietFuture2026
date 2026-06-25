package org.example.season.strategy;

import java.util.Set;

public interface StatusTransitionStrategy<T extends Enum<T>> {
    boolean canTransition(T currentStatus, T targetStatus);
    Set<T> getAllowedTransitions(T currentStatus);
    boolean isTerminalStatus(T status);
    T getInitialStatus();
}
