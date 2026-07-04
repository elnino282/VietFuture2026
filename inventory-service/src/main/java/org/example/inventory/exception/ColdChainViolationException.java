package org.example.inventory.exception;

public class ColdChainViolationException extends RuntimeException {
    public ColdChainViolationException(String message) {
        super(message);
    }
}
