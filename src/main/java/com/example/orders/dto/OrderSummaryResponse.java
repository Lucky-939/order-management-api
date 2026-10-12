package com.example.orders.dto;

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Summary DTO for order listings; deliberately omits items and item count to avoid triggering lazy N+1 queries
public class OrderSummaryResponse {

    private Long id;
    private String customerName;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;

    public OrderSummaryResponse() {
    }

    public OrderSummaryResponse(Long id, String customerName, OrderStatus status, LocalDateTime createdAt, BigDecimal totalAmount) {
        this.id = id;
        this.customerName = customerName;
        this.status = status;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
    }

    // Static factory creating summary DTO without accessing lazy relationships
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
            order.getId(),
            order.getCustomerName(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getTotalAmount()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
