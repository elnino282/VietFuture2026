package org.example.QuanLyMuaVu.module.shared.pattern.Chain;

/**
 * Chain of Responsibility Pattern: Base Handler for Validation Pipelines.
 * <p>
 * This abstract class provides the chain infrastructure, allowing validation
 * responsibilities to be passed along a chain of handlers. Each handler decides
 * either to process the request or pass it to the next handler.
 * <p>
 * Benefits:
 * - Decouples sender from receivers
 * - Flexible validation ordering
 * - Easy to add/remove validators without modifying existing code
 * - Each validator is independently testable
 *
 * @param <T> The type of object being validated
 */
public abstract class ValidationHandler<T> {

    private ValidationHandler<T> nextHandler;

    /**
     * Links the next handler in the chain.
     *
     * @param handler The next handler to process
     * @return The handler passed in (for chaining)
     */
    public ValidationHandler<T> setNext(ValidationHandler<T> handler) {
        this.nextHandler = handler;
        return handler;
    }

    /**
     * Returns the next handler in the chain (for testing/inspection).
     *
     * @return The next handler, or null if this is the last
     */
    public ValidationHandler<T> getNext() {
        return nextHandler;
    }

    /**
     * Validates the request and passes to the next handler if validation passes.
     * If validation fails, an AppException should be thrown.
     *
     * @param request The request to validate
     */
    public void validate(T request) {
        doValidate(request);
        if (nextHandler != null) {
            nextHandler.validate(request);
        }
    }

    /**
     * Template method for subclasses to implement specific validation logic.
     * Should throw AppException if validation fails.
     *
     * @param request The request to validate
     */
    protected abstract void doValidate(T request);

    /**
     * Returns a human-readable name for this validator (for logging/debugging).
     *
     * @return The validator name
     */
    public abstract String getValidatorName();
}
