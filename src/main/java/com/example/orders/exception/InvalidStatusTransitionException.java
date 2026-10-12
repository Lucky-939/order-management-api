package com.example.orders.exception;

// Extends RuntimeException so Spring's @Transactional mechanism automatically triggers rollback upon this exception
public class InvalidStatusTransitionException extends RuntimeException {

    // Initializes exception with a human-readable error description
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
