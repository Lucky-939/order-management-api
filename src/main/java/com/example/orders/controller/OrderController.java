package com.example.orders.controller;

import com.example.orders.dto.OrderResponse;
import com.example.orders.dto.OrderSummaryResponse;
import com.example.orders.dto.PagedResponse;
import com.example.orders.dto.PlaceOrderRequest;
import com.example.orders.dto.UpdateStatusRequest;
import com.example.orders.entity.OrderStatus;
import com.example.orders.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// REST controller exposing order-related API endpoints
// Do NOT put @Validated on the class: Spring 6.1 built-in method validation returns 400 for constraint annotations on parameters, while @Validated would throw ConstraintViolationException, which becomes 500 until Phase 5.
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    // Constructor injection ensures immutability and simplifies unit testing without reflection
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Accepts order creation requests, validates payload, delegates to service, and responds with 201 Created
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Retrieves an individual order with full item and product details by ID
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    // Returns a paginated list of order summaries filtered optionally by status, sorted deterministically by createdAt desc, id desc
    @GetMapping
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PagedResponse<OrderSummaryResponse> response = orderService.listOrders(status, page, size);
        return ResponseEntity.ok(response);
    }

    // Updates order lifecycle status after validation and delegates to service
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        OrderResponse response = orderService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    // Deletes an order and restores reserved stock, returning 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}

