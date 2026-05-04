package org.example.QuanLyMuaVu.module.shared.pattern.Chain;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain of Responsibility Pattern: Builder for Validation Chains.
 * <p>
 * Fluent builder that simplifies chain construction and ensures
 * type safety when building validation pipelines.
 *
 * @param <T> The type of object being validated
 */
public class ValidationChainBuilder<T> {

    private final List<ValidationHandler<T>> handlers = new ArrayList<>();

    /**
     * Adds a handler to the chain.
     *
     * @param handler The handler to add
     * @return This builder for fluent chaining
     */
    public ValidationChainBuilder<T> addHandler(ValidationHandler<T> handler) {
        handlers.add(handler);
        return this;
    }

    /**
     * Builds and returns the first handler in the chain.
     * All handlers are linked in the order they were added.
     *
     * @return The first handler, or null if no handlers were added
     */
    public ValidationHandler<T> build() {
        if (handlers.isEmpty()) {
            return null;
        }

        // Link handlers in sequence
        for (int i = 0; i < handlers.size() - 1; i++) {
            handlers.get(i).setNext(handlers.get(i + 1));
        }

        return handlers.get(0);
    }

    /**
     * Returns the number of handlers in the chain.
     *
     * @return Handler count
     */
    public int size() {
        return handlers.size();
    }
}
