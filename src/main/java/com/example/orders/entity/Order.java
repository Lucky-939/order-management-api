package com.example.orders.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Marks this class as a JPA entity
@Entity
// "orders" is explicitly used because "ORDER" is a reserved SQL keyword in MySQL and standard SQL
@Table(name = "orders")
public class Order {

    // Marks this field as the primary key
    @Id
    // Configures MySQL AUTO_INCREMENT strategy for primary key generation
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hibernate adds "and version=?" to every UPDATE/DELETE and increments it; if another transaction changed the row first, zero rows match and Hibernate throws an optimistic-locking failure
    @Version
    private long version;

    // Maps to customer_name column and enforces non-null constraint
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    // Stores the enum value as a string literal instead of integer ordinal for schema safety
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    // Maps creation timestamp, prevented from being changed on future updates
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Uses BigDecimal with fixed precision and scale for precise monetary calculation
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Replacing the list reference breaks orphanRemoval, so there is deliberately no setter
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // Required by JPA specification for entity reflection and proxy instantiation
    public Order() {
    }

    // Convenience constructor for creating a new order before persistence
    public Order(String customerName, OrderStatus status, BigDecimal totalAmount) {
        this.customerName = customerName;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    // Automatically assigns current timestamp right before the entity is first persisted
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Helper method that ensures bidirectional consistency on both sides of the relationship
    public void addItem(OrderItem item) {
        if (item != null) {
            this.items.add(item);
            item.setOrder(this);
        }
    }

    // Helper method to safely remove an item while maintaining relationship consistency
    public void removeItem(OrderItem item) {
        if (item != null) {
            this.items.remove(item);
            item.setOrder(null);
        }
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

    public List<OrderItem> getItems() {
        return items;
    }
}
