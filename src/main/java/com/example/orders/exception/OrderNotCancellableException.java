package com.example.orders.exception;

// Extends RuntimeException so Spring's @Transactional rolls back on unchecked exceptions
public class OrderNotCancellableException extends RuntimeException {

    // Initializes the exception with an error message describing why the order cannot be cancelled
    public OrderNotCancellableException(String message) {
        super(message);
    }
}
