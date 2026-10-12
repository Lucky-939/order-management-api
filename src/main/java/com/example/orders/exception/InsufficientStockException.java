package com.example.orders.exception;

// Extends RuntimeException so Spring's @Transactional mechanism automatically triggers rollback upon this exception
public class InsufficientStockException extends RuntimeException {

    // Initializes exception with a human-readable error description
    public InsufficientStockException(String message) {
        super(message);
    }
}
