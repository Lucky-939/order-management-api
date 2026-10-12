package com.example.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

// Marks this class as a JPA entity mapped to a database table
@Entity
// Explicitly maps this entity to the "products" table in the database
@Table(name = "products")
public class Product {

    // Marks this field as the primary key of the entity
    @Id
    // Configures MySQL AUTO_INCREMENT primary key generation strategy
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hibernate adds "and version=?" to every UPDATE/DELETE and increments it; if another transaction changed the row first, zero rows match and Hibernate throws an optimistic-locking failure
    @Version
    private long version;

    // Enforces a non-null constraint on the product name column
    @Column(name = "name", nullable = false)
    private String name;

    // Uses BigDecimal with fixed precision and scale for exact monetary values without floating-point errors
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Enforces a non-null constraint on the inventory stock column
    @Column(name = "stock", nullable = false)
    private int stock;

    // Required by JPA specification for entity instantiation via reflection
    public Product() {
    }

    // Convenience constructor for creating a new product without an ID
    public Product(String name, BigDecimal price, int stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
