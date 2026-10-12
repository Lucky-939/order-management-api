package com.example.orders.entity;

// Represents the lifecycle states of an order
public enum OrderStatus {
    // Initial status when an order has been successfully placed by a customer
    PLACED,

    // Status when an order has been dispatched from the warehouse
    SHIPPED,

    // Final status when the customer has received the order
    DELIVERED;

    // Legal transitions are defined in one place so the service has no scattered if-chains
    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) {
            return false;
        }
        switch (this) {
            case PLACED:
                return next == SHIPPED;
            case SHIPPED:
                return next == DELIVERED;
            case DELIVERED:
                return false;
            default:
                return false;
        }
    }
}
