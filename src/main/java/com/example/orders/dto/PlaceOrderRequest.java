package com.example.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// DTO representing the incoming payload to place a new order
public class PlaceOrderRequest {

    // Ensures customer name is non-null and contains at least one non-whitespace character
    @NotBlank(message = "customerName must not be blank")
    private String customerName;

    // Ensures the order contains at least one item, with nested validation and rejection of null elements
    @NotEmpty(message = "items list must not be empty")
    @Valid
    private List<@NotNull(message = "items must not contain null") @Valid OrderItemRequest> items;

    // Required for JSON deserialization frameworks
    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(String customerName, List<OrderItemRequest> items) {
        this.customerName = customerName;
        this.items = items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
