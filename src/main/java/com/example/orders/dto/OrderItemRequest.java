package com.example.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// DTO representing an individual item line in an order request
public class OrderItemRequest {

    // Boxed Long ensures missing JSON field deserializes to null and triggers validation failure
    @NotNull(message = "productId must not be null")
    private Long productId;

    // Boxed Integer prevents defaulting to 0 and allows @NotNull and @Min checks
    @NotNull(message = "quantity must not be null")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    // Required for JSON deserialization frameworks
    public OrderItemRequest() {
    }

    public OrderItemRequest(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
