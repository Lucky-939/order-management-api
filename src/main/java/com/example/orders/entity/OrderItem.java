package com.example.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

// Marks this class as a JPA entity
@Entity
// Maps this entity to the "order_items" table in the database
@Table(name = "order_items")
public class OrderItem {

    // Marks this field as the primary key
    @Id
    // Configures MySQL AUTO_INCREMENT primary key generation strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-one relationship to Order with lazy fetching to prevent eager queries
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Defines foreign key column "order_id" pointing to the orders table
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Many-to-one relationship to Product with lazy fetching to avoid loading product details unnecessarily
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Defines foreign key column "product_id" pointing to the products table
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Enforces non-null constraint on the ordered item count
    @Column(name = "quantity", nullable = false)
    private int quantity;

    // Stores price snapshot at the moment of order placement to isolate from subsequent product price edits
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // Required by JPA specification for entity instantiation via reflection
    public OrderItem() {
    }

    // Convenience constructor for creating an order item before persistence
    public OrderItem(Order order, Product product, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
