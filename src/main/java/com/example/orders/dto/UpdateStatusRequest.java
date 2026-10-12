package com.example.orders.dto;

import com.example.orders.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

// DTO representing the request payload to update an order's lifecycle status
public class UpdateStatusRequest {

    // Target status to update to, must not be null
    @NotNull(message = "status must not be null")
    private OrderStatus status;

    // Required for JSON deserialization by Jackson
    public UpdateStatusRequest() {
    }

    public UpdateStatusRequest(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
